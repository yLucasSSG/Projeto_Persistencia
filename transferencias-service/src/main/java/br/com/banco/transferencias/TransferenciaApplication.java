package br.com.banco.transferencias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microsservico Transferencias (PIX, TED e DOC). */
@SpringBootApplication
public class TransferenciaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferenciaApplication.class, args);
    }
}
