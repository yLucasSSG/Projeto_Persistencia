package br.com.banco.saldo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa a validacao e o debito do saldo e confere o que ficou gravado (H2 em memoria). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-saldo",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ContaPersistenciaTest {

    @Autowired ContaService service;
    @Autowired ContaRepository repository;

    @Test
    void contaCriadaFicaGravada() {
        Conta conta = service.criar("S-1", new BigDecimal("100.00"));

        Conta doBanco = repository.findById(conta.getId()).get();
        assertEquals("S-1", doBanco.getNumero());
        assertEquals(0, new BigDecimal("100.00").compareTo(doBanco.getSaldo()));
    }

    @Test
    void debitoComSaldoDesconta() {
        Conta conta = service.criar("S-2", new BigDecimal("100.00"));

        assertTrue(service.debitar(conta.getId(), new BigDecimal("40.00")));

        assertEquals(0, new BigDecimal("60.00").compareTo(repository.findById(conta.getId()).get().getSaldo()));
    }

    @Test
    void debitoSemSaldoRecusaENaoAlteraNada() {
        Conta conta = service.criar("S-3", new BigDecimal("10.00"));

        assertFalse(service.debitar(conta.getId(), new BigDecimal("50.00")));

        assertEquals(0, new BigDecimal("10.00").compareTo(repository.findById(conta.getId()).get().getSaldo()));
    }
}
