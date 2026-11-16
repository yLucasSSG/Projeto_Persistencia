package br.com.banco.transferencias.dto;

import br.com.banco.transferencias.model.ModalidadeTransferencia;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CriarTransferenciaRequest(
    @NotNull(message = "O identificador da transação é obrigatório.") UUID idTransacao,
    @NotNull(message = "A conta é obrigatória.") @Positive(message = "A conta deve ser positiva.") Long contaId,
    @NotBlank(message = "O destino é obrigatório.") @Size(max = 120, message = "O destino deve ter até 120 caracteres.") String destino,

    @NotNull(message = "O valor é obrigatório.")
    @DecimalMin(value = "0", inclusive = false, message = "O valor deve ser positivo.")
    @Digits(integer = 17, fraction = 2, message = "O valor deve ter até 17 dígitos inteiros e 2 casas decimais.") BigDecimal valor,
    @NotNull(message = "A modalidade é obrigatória.") ModalidadeTransferencia modalidade
) {
    public CriarTransferenciaRequest {
        if (destino != null) destino = destino.strip();

    }

}
