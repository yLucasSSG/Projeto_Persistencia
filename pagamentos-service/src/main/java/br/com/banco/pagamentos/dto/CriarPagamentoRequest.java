package br.com.banco.pagamentos.dto;

import br.com.banco.pagamentos.model.ModalidadePagamento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CriarPagamentoRequest(
    @NotNull(message = "O identificador da transação é obrigatório.") UUID idTransacao,
    @NotNull(message = "A conta é obrigatória.") @Positive(message = "A conta deve ser positiva.") Long contaId,
     @Size(max = 120, message = "O destino deve ter até 120 caracteres.") String destino,
    @Size(max = 120, message = "O código de barras deve ter até 120 caracteres.") String codigoBarras,
    @NotNull(message = "O valor é obrigatório.")
    @DecimalMin(value = "0", inclusive = false, message = "O valor deve ser positivo.")
    @Digits(integer = 17, fraction = 2, message = "O valor deve ter até 17 dígitos inteiros e 2 casas decimais.") BigDecimal valor,
    @NotNull(message = "A modalidade é obrigatória.") ModalidadePagamento modalidade
) {
    public CriarPagamentoRequest {
        if (destino != null) destino = destino.strip();
        if (codigoBarras != null) codigoBarras = codigoBarras.strip();
    }

    @AssertTrue(message = "Boleto exige somente codigoBarras; PIX e saldo em conta exigem somente destino.")
    public boolean isIdentificacaoValida() {
        if (modalidade == null) return true;
        if (modalidade == ModalidadePagamento.BOLETO) {
            return codigoBarras != null && !codigoBarras.isBlank() && destino == null;
        }
        return destino != null && !destino.isBlank() && codigoBarras == null;
    }

}
