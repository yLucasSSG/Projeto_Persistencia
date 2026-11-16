package br.com.banco.saldo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarContaRequest(
        @NotBlank(message = "O número da conta é obrigatório.")
        @Size(max = 30, message = "O número da conta deve ter até 30 caracteres.")
        String numero,

        @NotNull(message = "O saldo inicial é obrigatório.")
        @DecimalMin(value = "0.00", message = "O saldo inicial não pode ser negativo.")
        @Digits(integer = 17, fraction = 2,
                message = "O saldo deve ter até 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal saldoInicial
) {
    public CriarContaRequest {
        if (numero != null) {
            numero = numero.strip();
        }
    }
}
