package br.com.banco.saldo.service;

import br.com.banco.saldo.dto.ContaResponse;
import br.com.banco.saldo.dto.CriarContaRequest;
import br.com.banco.saldo.exception.ContaJaExisteException;
import br.com.banco.saldo.exception.ContaNaoEncontradaException;
import br.com.banco.saldo.model.Conta;
import br.com.banco.saldo.repository.ContaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
public class ContaService {

    private final ContaRepository repository;

    public ContaService(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContaResponse criar(CriarContaRequest dados) {
        try {
            Conta conta = new Conta(dados.numero(), dados.saldoInicial());
            return ContaResponse.de(repository.saveAndFlush(conta));
        } catch (DataIntegrityViolationException exception) {
            // A restrição única no MySQL também protege cadastros simultâneos.
            if (exception.getMostSpecificCause() instanceof SQLException causa
                    && causa.getErrorCode() == 1062) {
                throw new ContaJaExisteException(exception);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ContaResponse consultar(Long id) {
        Conta conta = repository.findById(id)
                .orElseThrow(ContaNaoEncontradaException::new);
        return ContaResponse.de(conta);
    }
}
