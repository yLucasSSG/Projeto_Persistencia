package br.com.banco.transferencias.service;

import br.com.banco.transferencias.model.*;
import br.com.banco.transferencias.repository.ComunicacaoRepository;
import org.springframework.stereotype.Component;

/** Adaptador didático: registra a comunicação no banco local, sem acessar o Banco Central real. */
@Component
public class BancoCentralSimulado {
    private final ComunicacaoRepository repository;
    public BancoCentralSimulado(ComunicacaoRepository repository) { this.repository = repository; }
    public ComunicacaoBancoCentral comunicar(Transferencia ordem) {
        return repository.save(new ComunicacaoBancoCentral(ordem));
    }
}
