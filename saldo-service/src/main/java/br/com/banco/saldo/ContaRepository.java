package br.com.banco.saldo;

import org.springframework.data.jpa.repository.JpaRepository;

/** O Spring Data cria sozinho save, findById, findAll etc. Nao precisa escrever SQL. */
public interface ContaRepository extends JpaRepository<Conta, Long> {
}
