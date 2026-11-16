package br.com.banco.saldo.dto;

import br.com.banco.saldo.model.Conta;

import java.math.BigDecimal;

public record ContaResponse(Long id, String numero, BigDecimal saldo) {

    public static ContaResponse de(Conta conta) {
        return new ContaResponse(conta.getId(), conta.getNumero(), conta.getSaldo());
    }
}
