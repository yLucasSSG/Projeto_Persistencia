package br.com.banco.saldo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Conta bancaria. Vira a tabela "contas". */
@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // o MySQL gera o id (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    protected Conta() {
        // exigido pelo JPA
    }

    public Conta(String numero, BigDecimal saldo) {
        this.numero = numero;
        this.saldo = saldo;
    }

    /** Valida o saldo e debita. Retorna false (sem alterar nada) se o saldo for insuficiente. */
    public boolean debitar(BigDecimal valor) {
        if (saldo.compareTo(valor) < 0) {
            return false;
        }
        saldo = saldo.subtract(valor);
        return true;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public BigDecimal getSaldo() { return saldo; }
}
