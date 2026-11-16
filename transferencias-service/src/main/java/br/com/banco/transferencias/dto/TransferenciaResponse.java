package br.com.banco.transferencias.dto;

import br.com.banco.transferencias.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferenciaResponse(UUID idTransacao, Long contaId, String destino,
         BigDecimal valor, ModalidadeTransferencia modalidade, SituacaoOperacao situacao,
        String protocoloBancoCentral, Instant criadaEm) {
    public static TransferenciaResponse de(Transferencia ordem, ComunicacaoBancoCentral comunicacao) {
        return new TransferenciaResponse(ordem.getIdTransacao(), ordem.getContaId(), ordem.getDestino(),
                 ordem.getValor(), ordem.getModalidade(), ordem.getSituacao(),
                comunicacao == null ? null : comunicacao.getProtocolo(), ordem.getCriadaEm());
    }
}
