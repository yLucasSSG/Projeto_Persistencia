package br.com.banco.transferencias.repository;

import br.com.banco.transferencias.model.Transferencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface TransferenciaRepository extends JpaRepository<Transferencia, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Transferencia o where o.idTransacao = :id")
    Optional<Transferencia> buscarParaProcessar(@Param("id") UUID id);
}
