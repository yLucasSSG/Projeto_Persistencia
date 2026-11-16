package br.com.banco.saldo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    protected Conta() {
        // Construtor utilizado pelo JPA para carregar os dados do banco.
    }

    public Conta(String numero, BigDecimal saldo) {
        this.numero = numero;
        this.saldo = saldo;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
