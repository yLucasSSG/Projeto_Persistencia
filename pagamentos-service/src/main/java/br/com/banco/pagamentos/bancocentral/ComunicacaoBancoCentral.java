package br.com.banco.pagamentos.bancocentral;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro de cada transacao comunicada ao Banco Central. Vira a tabela "comunicacoes_banco_central". */
@Entity
@Table(name = "comunicacoes_banco_central")
public class ComunicacaoBancoCentral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoOperacao;   // TRANSFERENCIA ou PAGAMENTO

    @Column(nullable = false)
    private Long idOperacao;       // id da transferencia ou do pagamento

    @Column(nullable = false)
    private String modalidade;     // PIX, TED, DOC, BOLETO, SALDO_EM_CONTA

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String protocolo;      // confirmacao devolvida pelo Banco Central

    @Column(nullable = false)
    private LocalDateTime dataHora;

    protected ComunicacaoBancoCentral() {
    }

    public ComunicacaoBancoCentral(String tipoOperacao, Long idOperacao, String modalidade,
                                   BigDecimal valor, String protocolo) {
        this.tipoOperacao = tipoOperacao;
        this.idOperacao = idOperacao;
        this.modalidade = modalidade;
        this.valor = valor;
        this.protocolo = protocolo;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTipoOperacao() { return tipoOperacao; }
    public Long getIdOperacao() { return idOperacao; }
    public String getModalidade() { return modalidade; }
    public BigDecimal getValor() { return valor; }
    public String getProtocolo() { return protocolo; }
    public LocalDateTime getDataHora() { return dataHora; }
}
