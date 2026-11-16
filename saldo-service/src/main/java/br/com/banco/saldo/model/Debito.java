package br.com.banco.saldo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "debitos")
public class Debito {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID idTransacao;

    @Column(nullable = false)
    private Long contaId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 200)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ResultadoDebito resultado;

    protected Debito() {
    }

    public Debito(UUID idTransacao, Long contaId, BigDecimal valor, String referencia, ResultadoDebito resultado) {
        this.idTransacao = idTransacao;
        this.contaId = contaId;
        this.valor = valor;
        this.referencia = referencia;
        this.resultado = resultado;
    }

    public UUID getIdTransacao() { return idTransacao; }
    public Long getContaId() { return contaId; }
    public BigDecimal getValor() { return valor; }
    public String getReferencia() { return referencia; }
    public ResultadoDebito getResultado() { return resultado; }
}
