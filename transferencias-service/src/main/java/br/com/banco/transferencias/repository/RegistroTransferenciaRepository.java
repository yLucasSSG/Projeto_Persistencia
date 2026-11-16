package br.com.banco.transferencias.repository;

import br.com.banco.transferencias.dto.CriarTransferenciaRequest;
import br.com.banco.transferencias.model.Transferencia;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RegistroTransferenciaRepository {
    private final EntityManager entityManager;
    public RegistroTransferenciaRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional
    public void registrar(CriarTransferenciaRequest dados) {
        // persist faz INSERT: uma repetição nunca sobrescreve os dados da ordem existente.
        entityManager.persist(new Transferencia(dados));
        entityManager.flush();
    }
}
