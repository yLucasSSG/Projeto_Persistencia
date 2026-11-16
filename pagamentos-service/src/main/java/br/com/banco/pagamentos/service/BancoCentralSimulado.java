package br.com.banco.pagamentos.service;

import br.com.banco.pagamentos.model.*;
import br.com.banco.pagamentos.repository.ComunicacaoRepository;
import org.springframework.stereotype.Component;

/** Adaptador didático: registra a comunicação no banco local, sem acessar o Banco Central real. */
@Component
public class BancoCentralSimulado {
    private final ComunicacaoRepository repository;
    public BancoCentralSimulado(ComunicacaoRepository repository) { this.repository = repository; }
    public ComunicacaoBancoCentral comunicar(Pagamento ordem) {
        return repository.save(new ComunicacaoBancoCentral(ordem));
    }
}
