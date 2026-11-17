package br.com.banco.pagamentos.bancocentral;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicacaoRepository extends JpaRepository<ComunicacaoBancoCentral, Long> {
}
