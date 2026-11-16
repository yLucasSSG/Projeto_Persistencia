package br.com.banco.pagamentos.repository;

import br.com.banco.pagamentos.model.ComunicacaoBancoCentral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ComunicacaoRepository extends JpaRepository<ComunicacaoBancoCentral, UUID> { }
