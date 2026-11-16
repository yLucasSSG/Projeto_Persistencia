package br.com.banco.transferencias.service;

import br.com.banco.transferencias.dto.*;
import br.com.banco.transferencias.exception.OperacaoException;
import br.com.banco.transferencias.model.*;
import br.com.banco.transferencias.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessamentoTransferenciaService {
    private final TransferenciaRepository ordens;
    private final ComunicacaoRepository comunicacoes;
    private final SaldoClient saldo;
    private final BancoCentralSimulado bancoCentral;

    public ProcessamentoTransferenciaService(TransferenciaRepository ordens, ComunicacaoRepository comunicacoes,
            SaldoClient saldo, BancoCentralSimulado bancoCentral) {
        this.ordens = ordens;
        this.comunicacoes = comunicacoes;
        this.saldo = saldo;
        this.bancoCentral = bancoCentral;
    }
    @Transactional
    public TransferenciaResponse processar(CriarTransferenciaRequest dados) {
        Transferencia ordem = ordens.buscarParaProcessar(dados.idTransacao())
                .orElseThrow(() -> new OperacaoException(404, "Operação não encontrada."));
        if (!ordem.correspondeA(dados)) {
            throw new OperacaoException(409, "O identificador já foi utilizado com outros dados.");
        }
        if (ordem.getSituacao() == SituacaoOperacao.PENDENTE) {
            boolean autorizado = saldo.debitar(ordem);
            if (autorizado) bancoCentral.comunicar(ordem);
            ordem.concluir(autorizado);
        }
        return TransferenciaResponse.de(ordem, comunicacoes.findById(ordem.getIdTransacao()).orElse(null));
    }
}
