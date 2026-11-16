package br.com.banco.saldo.service;

import br.com.banco.saldo.dto.DebitoRequest;
import br.com.banco.saldo.dto.DebitoResponse;
import br.com.banco.saldo.exception.ContaNaoEncontradaException;
import br.com.banco.saldo.exception.TransacaoConflitanteException;
import br.com.banco.saldo.model.Conta;
import br.com.banco.saldo.model.Debito;
import br.com.banco.saldo.model.ResultadoDebito;
import br.com.banco.saldo.repository.ContaRepository;
import br.com.banco.saldo.repository.DebitoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
public class DebitoService {

    private final ContaRepository contas;
    private final DebitoRepository debitos;

    public DebitoService(ContaRepository contas, DebitoRepository debitos) {
        this.contas = contas;
        this.debitos = debitos;
    }

    @Transactional
    public DebitoResponse debitar(Long contaId, DebitoRequest dados) {
        // O bloqueio no MySQL serializa movimentações da mesma conta.
        Conta conta = contas.buscarParaDebito(contaId).orElseThrow(ContaNaoEncontradaException::new);
        Debito anterior = debitos.findById(dados.idTransacao()).orElse(null);
        if (anterior != null) {
            if (!anterior.getContaId().equals(contaId)
                    || anterior.getValor().compareTo(dados.valor()) != 0
                    || !anterior.getReferencia().equals(dados.referencia())) {
                throw new TransacaoConflitanteException();
            }
            return DebitoResponse.de(anterior);
        }

        ResultadoDebito resultado = conta.debitar(dados.valor())
                ? ResultadoDebito.AUTORIZADO : ResultadoDebito.SALDO_INSUFICIENTE;
        try {
            Debito debito = debitos.saveAndFlush(new Debito(
                    dados.idTransacao(), contaId, dados.valor(), dados.referencia(), resultado));
            return DebitoResponse.de(debito);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException causa
                    && causa.getErrorCode() == 1062) {
                // A transação inteira é revertida, inclusive a alteração do saldo.
                throw new TransacaoConflitanteException();
            }
            throw exception;
        }
    }
}
