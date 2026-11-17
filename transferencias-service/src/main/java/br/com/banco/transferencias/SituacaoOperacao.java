package br.com.banco.transferencias;

/** Resultado de uma transferencia ou pagamento (os dois caminhos dos diagramas de sequencia). */
public enum SituacaoOperacao {
    CONCLUIDA,          // saldo suficiente: debitou e comunicou o Banco Central
    SALDO_INSUFICIENTE  // saldo insuficiente: nada foi debitado
}
