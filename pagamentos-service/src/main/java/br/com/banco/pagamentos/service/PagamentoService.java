package br.com.banco.pagamentos.service;

import br.com.banco.pagamentos.dto.*;
import br.com.banco.pagamentos.exception.OperacaoException;
import br.com.banco.pagamentos.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.util.UUID;

@Service
public class PagamentoService {
    private final RegistroPagamentoRepository registro;
    private final ProcessamentoPagamentoService processamento;
    private final PagamentoRepository ordens;
    private final ComunicacaoRepository comunicacoes;

    public PagamentoService(RegistroPagamentoRepository registro, ProcessamentoPagamentoService processamento,
            PagamentoRepository ordens, ComunicacaoRepository comunicacoes) {
        this.registro = registro;
        this.processamento = processamento;
        this.ordens = ordens;
        this.comunicacoes = comunicacoes;
    }
    public PagamentoResponse criar(CriarPagamentoRequest dados) {
        try {
            registro.registrar(dados);
        } catch (DataIntegrityViolationException exception) {
            if (!(exception.getMostSpecificCause() instanceof SQLException causa) || causa.getErrorCode() != 1062) {
                throw exception;
            }
        }
        // A ordem já está confirmada no MySQL antes da chamada remota a Saldo.
        return processamento.processar(dados);
    }
    @Transactional(readOnly = true)
    public PagamentoResponse consultar(UUID id) {
        var ordem = ordens.findById(id).orElseThrow(() -> new OperacaoException(404, "Operação não encontrada."));
        return PagamentoResponse.de(ordem, comunicacoes.findById(id).orElse(null));
    }
}
