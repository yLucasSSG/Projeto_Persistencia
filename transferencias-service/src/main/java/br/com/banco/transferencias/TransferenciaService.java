package br.com.banco.transferencias;

import br.com.banco.transferencias.bancocentral.BancoCentralService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class TransferenciaService {

    private final TransferenciaRepository repository;
    private final SaldoClient saldo;
    private final BancoCentralService bancoCentral;

    public TransferenciaService(TransferenciaRepository repository, SaldoClient saldo,
                                BancoCentralService bancoCentral) {
        this.repository = repository;
        this.saldo = saldo;
        this.bancoCentral = bancoCentral;
    }

    /**
     * Segue o diagrama de sequencia de transferencia.
     * @Transactional: a transferencia e a comunicacao ao Banco Central sao gravadas juntas.
     */
    @Transactional
    public Transferencia transferir(Long contaId, String destino, BigDecimal valor, ModalidadeTransferencia modalidade) {
        if (contaId == null || destino == null || destino.isBlank() || valor == null || valor.signum() <= 0
                || modalidade == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe contaId, destino, modalidade e um valor maior que zero.");
        }
        Transferencia transferencia = new Transferencia(contaId, destino.strip(), valor, modalidade);

        // 1.1 validarEDebitar: chama o microsservico Saldo (se nao tiver saldo, nada e alterado)
        boolean debitoAutorizado = saldo.debitar(contaId, valor);
        transferencia.definirResultado(debitoAutorizado ? SituacaoOperacao.CONCLUIDA : SituacaoOperacao.SALDO_INSUFICIENTE);
        transferencia = repository.save(transferencia); // grava a transferencia (gera o id)

        if (debitoAutorizado) {
            // caminho [debitoAutorizado]: comunica o Banco Central e guarda o protocolo
            transferencia.definirProtocolo(
                    bancoCentral.comunicar("TRANSFERENCIA", transferencia.getId(), modalidade.name(), valor));
        }
        // caminho [saldoInsuficiente]: nada e comunicado
        return transferencia;
    }

    public Transferencia consultar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transferencia nao encontrada."));
    }
}
