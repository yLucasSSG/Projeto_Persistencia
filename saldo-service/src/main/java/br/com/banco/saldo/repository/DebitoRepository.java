package br.com.banco.saldo.repository;

import br.com.banco.saldo.model.Debito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DebitoRepository extends JpaRepository<Debito, UUID> {
}
