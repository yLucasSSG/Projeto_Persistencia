package br.com.banco.saldo.exception;

public class TransacaoConflitanteException extends RuntimeException {

    public TransacaoConflitanteException() {
        super("O identificador da transação já foi utilizado com outros dados.");
    }
}
