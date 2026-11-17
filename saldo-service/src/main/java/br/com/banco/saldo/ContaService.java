package br.com.banco.saldo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class ContaService {

    private final ContaRepository repository;

    public ContaService(ContaRepository repository) {
        this.repository = repository;
    }

    public Conta criar(String numero, BigDecimal saldoInicial) {
        if (numero == null || numero.isBlank() || saldoInicial == null || saldoInicial.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o numero e um saldo inicial >= 0.");
        }
        return repository.save(new Conta(numero.strip(), saldoInicial));
    }

    /** Consulta de saldo (diagrama 01). */
    public Conta consultar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta nao encontrada."));
    }

    /**
     * "validarEDebitarAtomicamente" dos diagramas: chamado pelos servicos de Transferencias e Pagamentos.
     * Retorna true se debitou, false se o saldo e insuficiente (e nada foi alterado).
     * @Transactional: a validacao e o debito sao gravados juntos.
     */
    @Transactional
    public boolean debitar(Long id, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor deve ser maior que zero.");
        }
        return consultar(id).debitar(valor);
    }
}
