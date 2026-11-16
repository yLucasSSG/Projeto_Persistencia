package br.com.banco.transferencias.repository;

import br.com.banco.transferencias.model.ComunicacaoBancoCentral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ComunicacaoRepository extends JpaRepository<ComunicacaoBancoCentral, UUID> { }
