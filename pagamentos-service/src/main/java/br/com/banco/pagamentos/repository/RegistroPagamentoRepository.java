package br.com.banco.pagamentos.repository;

import br.com.banco.pagamentos.dto.CriarPagamentoRequest;
import br.com.banco.pagamentos.model.Pagamento;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RegistroPagamentoRepository {
    private final EntityManager entityManager;
    public RegistroPagamentoRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional
    public void registrar(CriarPagamentoRequest dados) {
        // persist faz INSERT: uma repetição nunca sobrescreve os dados da ordem existente.
        entityManager.persist(new Pagamento(dados));
        entityManager.flush();
    }
}
