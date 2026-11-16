package br.com.banco.pagamentos.repository;

import br.com.banco.pagamentos.model.Pagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Pagamento o where o.idTransacao = :id")
    Optional<Pagamento> buscarParaProcessar(@Param("id") UUID id);
}
