package br.com.banco.saldo.dto;

import br.com.banco.saldo.model.Debito;
import br.com.banco.saldo.model.ResultadoDebito;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitoResponse(UUID idTransacao, Long contaId, BigDecimal valor, ResultadoDebito resultado) {

    public static DebitoResponse de(Debito debito) {
        return new DebitoResponse(debito.getIdTransacao(), debito.getContaId(),
                debito.getValor(), debito.getResultado());
    }
}
