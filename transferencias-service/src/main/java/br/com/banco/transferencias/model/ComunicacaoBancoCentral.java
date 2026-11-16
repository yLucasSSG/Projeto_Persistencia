package br.com.banco.transferencias.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comunicacoes_banco_central")
public class ComunicacaoBancoCentral {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID idTransacao;
    @Column(nullable = false, length = 50)
    private String protocolo;
    @Column(nullable = false, length = 25)
    private String modalidade;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private Instant comunicadaEm;

    protected ComunicacaoBancoCentral() { }

    public ComunicacaoBancoCentral(Transferencia ordem) {
        idTransacao = ordem.getIdTransacao();
        protocolo = "SIM-" + idTransacao;
        modalidade = ordem.getModalidade().name();
        valor = ordem.getValor();
        comunicadaEm = Instant.now();
    }
    public UUID getIdTransacao() { return idTransacao; }
    public String getProtocolo() { return protocolo; }
    public String getModalidade() { return modalidade; }
    public BigDecimal getValor() { return valor; }
    public Instant getComunicadaEm() { return comunicadaEm; }
}
