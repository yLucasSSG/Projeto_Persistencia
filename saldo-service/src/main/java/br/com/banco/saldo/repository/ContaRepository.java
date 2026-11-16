package br.com.banco.saldo.repository;

import br.com.banco.saldo.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    Optional<Conta> findByNumero(String numero);
}
