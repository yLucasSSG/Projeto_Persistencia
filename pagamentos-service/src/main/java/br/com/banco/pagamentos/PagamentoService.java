package br.com.banco.pagamentos;

import br.com.banco.pagamentos.bancocentral.BancoCentralService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    private final PagamentoRepository repository;
    private final SaldoClient saldo;
    private final BancoCentralService bancoCentral;

    public PagamentoService(PagamentoRepository repository, SaldoClient saldo, BancoCentralService bancoCentral) {
        this.repository = repository;
        this.saldo = saldo;
        this.bancoCentral = bancoCentral;
    }

    /**
     * Segue o diagrama de sequencia de pagamento (PIX, boleto e saldo em conta tem o mesmo fluxo).
     * @Transactional: o pagamento e a comunicacao ao Banco Central sao gravados juntos.
     */
    @Transactional
    public Pagamento pagar(Long contaId, String destino, BigDecimal valor, ModalidadePagamento modalidade) {
        if (contaId == null || destino == null || destino.isBlank() || valor == null || valor.signum() <= 0
                || modalidade == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe contaId, destino, modalidade e um valor maior que zero.");
        }
        Pagamento pagamento = new Pagamento(contaId, destino.strip(), valor, modalidade);

        // 1.1 validarEDebitar: chama o microsservico Saldo (se nao tiver saldo, nada e alterado)
        boolean debitoAutorizado = saldo.debitar(contaId, valor);
        pagamento.definirResultado(debitoAutorizado ? SituacaoOperacao.CONCLUIDA : SituacaoOperacao.SALDO_INSUFICIENTE);
        pagamento = repository.save(pagamento); // grava o pagamento (gera o id)

        if (debitoAutorizado) {
            // caminho [debitoAutorizado]: comunica o Banco Central e guarda o protocolo
            pagamento.definirProtocolo(
                    bancoCentral.comunicar("PAGAMENTO", pagamento.getId(), modalidade.name(), valor));
        }
        // caminho [saldoInsuficiente]: nada e comunicado
        return pagamento;
    }

    public Pagamento consultar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento nao encontrado."));
    }
}
