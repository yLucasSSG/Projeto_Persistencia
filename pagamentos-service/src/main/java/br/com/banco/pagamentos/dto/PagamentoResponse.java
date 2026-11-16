package br.com.banco.pagamentos.dto;

import br.com.banco.pagamentos.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagamentoResponse(UUID idTransacao, Long contaId, String destino,
        String codigoBarras, BigDecimal valor, ModalidadePagamento modalidade, SituacaoOperacao situacao,
        String protocoloBancoCentral, Instant criadaEm) {
    public static PagamentoResponse de(Pagamento ordem, ComunicacaoBancoCentral comunicacao) {
        return new PagamentoResponse(ordem.getIdTransacao(), ordem.getContaId(), ordem.getDestino(),
                ordem.getCodigoBarras(), ordem.getValor(), ordem.getModalidade(), ordem.getSituacao(),
                comunicacao == null ? null : comunicacao.getProtocolo(), ordem.getCriadaEm());
    }
}
