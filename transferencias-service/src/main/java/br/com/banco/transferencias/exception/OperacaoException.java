package br.com.banco.transferencias.exception;

public class OperacaoException extends RuntimeException {
    private final int status;
    public OperacaoException(int status, String mensagem) { super(mensagem); this.status = status; }
    public int getStatus() { return status; }
}
