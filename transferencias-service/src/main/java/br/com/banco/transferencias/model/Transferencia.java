package br.com.banco.transferencias.model;

import br.com.banco.transferencias.dto.CriarTransferenciaRequest;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transferencias")
public class Transferencia {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID idTransacao;
    @Column(nullable = false)
    private Long contaId;
    @Column(nullable = false, length = 120)
    private String destino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private ModalidadeTransferencia modalidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private SituacaoOperacao situacao;
    @Column(nullable = false)
    private Instant criadaEm;

    protected Transferencia() { }

    public Transferencia(CriarTransferenciaRequest dados) {
        idTransacao = dados.idTransacao();
        contaId = dados.contaId();
        destino = dados.destino();

        valor = dados.valor();
        modalidade = dados.modalidade();
        situacao = SituacaoOperacao.PENDENTE;
        criadaEm = Instant.now();
    }

    public boolean correspondeA(CriarTransferenciaRequest dados) {
        return contaId.equals(dados.contaId()) && Objects.equals(destino, dados.destino())

                && valor.compareTo(dados.valor()) == 0 && modalidade == dados.modalidade();
    }

    public String referenciaDebito() { return "TRANSFERENCIA:" + modalidade + ":" + destino; }
    public void concluir(boolean autorizado) {
        situacao = autorizado ? SituacaoOperacao.CONCLUIDA : SituacaoOperacao.SALDO_INSUFICIENTE;
    }
    public UUID getIdTransacao() { return idTransacao; }
    public Long getContaId() { return contaId; }
    public String getDestino() { return destino; }

    public BigDecimal getValor() { return valor; }
    public ModalidadeTransferencia getModalidade() { return modalidade; }
    public SituacaoOperacao getSituacao() { return situacao; }
    public Instant getCriadaEm() { return criadaEm; }
}
