package br.com.banco.pagamentos.bancocentral;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simula o servico externo do Banco Central: nao existe API real,
 * entao so gera um protocolo e grava a comunicacao no banco.
 */
@Service
public class BancoCentralService {

    private final ComunicacaoRepository repository;

    public BancoCentralService(ComunicacaoRepository repository) {
        this.repository = repository;
    }

    /** "comunicarTransferencia / comunicarPagamento" dos diagramas. Devolve o protocolo de confirmacao. */
    public String comunicar(String tipoOperacao, Long idOperacao, String modalidade, BigDecimal valor) {
        String protocolo = UUID.randomUUID().toString();
        repository.save(new ComunicacaoBancoCentral(tipoOperacao, idOperacao, modalidade, valor, protocolo));
        return protocolo;
    }
}
