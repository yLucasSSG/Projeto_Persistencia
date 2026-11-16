package br.com.banco.saldo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitoRequest(
        @NotNull(message = "O identificador da transação é obrigatório.") UUID idTransacao,
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0", inclusive = false, message = "O valor deve ser positivo.")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter até 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal valor,
        @NotBlank(message = "A referência da operação é obrigatória.")
        @Size(max = 200, message = "A referência deve ter até 200 caracteres.")
        String referencia
) {
}
