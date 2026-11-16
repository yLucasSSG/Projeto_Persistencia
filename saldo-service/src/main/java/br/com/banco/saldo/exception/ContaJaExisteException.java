package br.com.banco.saldo.exception;

public class ContaJaExisteException extends RuntimeException {

    public ContaJaExisteException(Throwable causa) {
        super("Já existe uma conta com esse número.", causa);
    }
}
