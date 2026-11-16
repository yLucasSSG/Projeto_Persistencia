package br.com.banco.pagamentos.model;

import br.com.banco.pagamentos.dto.CriarPagamentoRequest;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
public class Pagamento {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID idTransacao;
    @Column(nullable = false)
    private Long contaId;
    @Column( length = 120)
    private String destino;
    @Column(length = 120)
    private String codigoBarras;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private ModalidadePagamento modalidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private SituacaoOperacao situacao;
    @Column(nullable = false)
    private Instant criadaEm;

    protected Pagamento() { }

    public Pagamento(CriarPagamentoRequest dados) {
        idTransacao = dados.idTransacao();
        contaId = dados.contaId();
        destino = dados.destino();
        codigoBarras = dados.codigoBarras();
        valor = dados.valor();
        modalidade = dados.modalidade();
        situacao = SituacaoOperacao.PENDENTE;
        criadaEm = Instant.now();
    }

    public boolean correspondeA(CriarPagamentoRequest dados) {
        return contaId.equals(dados.contaId()) && Objects.equals(destino, dados.destino())
                && Objects.equals(codigoBarras, dados.codigoBarras())
                && valor.compareTo(dados.valor()) == 0 && modalidade == dados.modalidade();
    }

    public String referenciaDebito() { return "PAGAMENTO:" + modalidade + ":" + (modalidade == ModalidadePagamento.BOLETO ? codigoBarras : destino); }
    public void concluir(boolean autorizado) {
        situacao = autorizado ? SituacaoOperacao.CONCLUIDA : SituacaoOperacao.SALDO_INSUFICIENTE;
    }
    public UUID getIdTransacao() { return idTransacao; }
    public Long getContaId() { return contaId; }
    public String getDestino() { return destino; }
    public String getCodigoBarras() { return codigoBarras; }
    public BigDecimal getValor() { return valor; }
    public ModalidadePagamento getModalidade() { return modalidade; }
    public SituacaoOperacao getSituacao() { return situacao; }
    public Instant getCriadaEm() { return criadaEm; }
}
