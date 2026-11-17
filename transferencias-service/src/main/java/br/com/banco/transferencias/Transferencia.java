package br.com.banco.transferencias;

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

/** Transferencia PIX, TED ou DOC. Vira a tabela "transferencias". */
@Entity
@Table(name = "transferencias")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // So o numero (id) da conta: a conta mora no banco do servico Saldo, entao nao ha chave estrangeira.
    @Column(nullable = false)
    private Long contaId;

    @Column(nullable = false)
    private String destino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING) // grava o nome ("PIX") e nao o numero
    @Column(nullable = false)
    private ModalidadeTransferencia modalidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoOperacao situacao;

    private String protocoloBancoCentral; // so existe quando foi concluida

    @Column(nullable = false)
    private LocalDateTime dataHora;

    protected Transferencia() {
    }

    public Transferencia(Long contaId, String destino, BigDecimal valor, ModalidadeTransferencia modalidade) {
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
    public ModalidadeTransferencia getModalidade() { return modalidade; }
    public SituacaoOperacao getSituacao() { return situacao; }
    public String getProtocoloBancoCentral() { return protocoloBancoCentral; }
    public LocalDateTime getDataHora() { return dataHora; }
}
