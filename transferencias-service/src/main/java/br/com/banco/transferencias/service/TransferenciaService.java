package br.com.banco.transferencias.service;

import br.com.banco.transferencias.dto.*;
import br.com.banco.transferencias.exception.OperacaoException;
import br.com.banco.transferencias.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.util.UUID;

@Service
public class TransferenciaService {
    private final RegistroTransferenciaRepository registro;
    private final ProcessamentoTransferenciaService processamento;
    private final TransferenciaRepository ordens;
    private final ComunicacaoRepository comunicacoes;

    public TransferenciaService(RegistroTransferenciaRepository registro, ProcessamentoTransferenciaService processamento,
            TransferenciaRepository ordens, ComunicacaoRepository comunicacoes) {
        this.registro = registro;
        this.processamento = processamento;
        this.ordens = ordens;
        this.comunicacoes = comunicacoes;
    }
    public TransferenciaResponse criar(CriarTransferenciaRequest dados) {
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
    public TransferenciaResponse consultar(UUID id) {
        var ordem = ordens.findById(id).orElseThrow(() -> new OperacaoException(404, "Operação não encontrada."));
        return TransferenciaResponse.de(ordem, comunicacoes.findById(id).orElse(null));
    }
}
