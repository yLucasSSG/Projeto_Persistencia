package br.com.banco.saldo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microsservico Saldo: guarda as contas, consulta o saldo e valida/debita para os outros servicos. */
@SpringBootApplication
public class SaldoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaldoApplication.class, args);
    }
}
