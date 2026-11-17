package br.com.banco.pagamentos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento via PIX, boleto ou saldo em conta. Vira a tabela "pagamentos". */
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // So o numero (id) da conta: a conta mora no banco do servico Saldo, entao nao ha chave estrangeira.
    @Column(nullable = false)
    private Long contaId;

    // Chave PIX, codigo de barras do boleto ou destino do pagamento com saldo em conta.
    @Column(nullable = false)
    private String destino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadePagamento modalidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoOperacao situacao;

    private String protocoloBancoCentral; // so existe quando foi concluido

    @Column(nullable = false)
    private LocalDateTime dataHora;

    protected Pagamento() {
    }

    public Pagamento(Long contaId, String destino, BigDecimal valor, ModalidadePagamento modalidade) {
        this.contaId = contaId;
        this.destino = destino;
        this.valor = valor;
        this.modalidade = modalidade;
        this.dataHora = LocalDateTime.now();
    }

    public void definirResultado(SituacaoOperacao situacao) { this.situacao = situacao; }
    public void definirProtocolo(String protocolo) { this.protocoloBancoCentral = protocolo; }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public String getDestino() { return destino; }
    public BigDecimal getValor() { return valor; }
    public ModalidadePagamento getModalidade() { return modalidade; }
    public SituacaoOperacao getSituacao() { return situacao; }
    public String getProtocoloBancoCentral() { return protocoloBancoCentral; }
    public LocalDateTime getDataHora() { return dataHora; }
}
