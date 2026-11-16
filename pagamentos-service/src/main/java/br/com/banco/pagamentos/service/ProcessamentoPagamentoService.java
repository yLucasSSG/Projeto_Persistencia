package br.com.banco.pagamentos.service;

import br.com.banco.pagamentos.dto.*;
import br.com.banco.pagamentos.exception.OperacaoException;
import br.com.banco.pagamentos.model.*;
import br.com.banco.pagamentos.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessamentoPagamentoService {
    private final PagamentoRepository ordens;
    private final ComunicacaoRepository comunicacoes;
    private final SaldoClient saldo;
    private final BancoCentralSimulado bancoCentral;

    public ProcessamentoPagamentoService(PagamentoRepository ordens, ComunicacaoRepository comunicacoes,
            SaldoClient saldo, BancoCentralSimulado bancoCentral) {
        this.ordens = ordens;
        this.comunicacoes = comunicacoes;
        this.saldo = saldo;
        this.bancoCentral = bancoCentral;
    }
    @Transactional
    public PagamentoResponse processar(CriarPagamentoRequest dados) {
        Pagamento ordem = ordens.buscarParaProcessar(dados.idTransacao())
                .orElseThrow(() -> new OperacaoException(404, "Operação não encontrada."));
        if (!ordem.correspondeA(dados)) {
            throw new OperacaoException(409, "O identificador já foi utilizado com outros dados.");
        }
        if (ordem.getSituacao() == SituacaoOperacao.PENDENTE) {
            boolean autorizado = saldo.debitar(ordem);
            if (autorizado) bancoCentral.comunicar(ordem);
            ordem.concluir(autorizado);
        }
        return PagamentoResponse.de(ordem, comunicacoes.findById(ordem.getIdTransacao()).orElse(null));
    }
}
