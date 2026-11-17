package br.com.banco.transferencias;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/** Faz a chamada HTTP ao microsservico Saldo ("Valida e debita saldo" do diagrama de pacotes). */
@Component
public class SaldoClient {

    /** Resposta do Saldo: autorizado=false significa saldo insuficiente. */
    private record ResultadoDebito(boolean autorizado) { }

    private final RestClient client;

    public SaldoClient(@Value("${saldo.url}") String urlDoSaldo) {
        this.client = RestClient.create(urlDoSaldo);
    }

    /** Pede ao Saldo para validar e debitar. Retorna true se debitou, false se o saldo e insuficiente. */
    public boolean debitar(Long contaId, BigDecimal valor) {
        try {
            ResultadoDebito resultado = client.post()
                    .uri("/contas/{id}/debitos", contaId)
                    .body(Map.of("valor", valor))
                    .retrieve()
                    .body(ResultadoDebito.class);
            return resultado != null && resultado.autorizado();
        } catch (HttpClientErrorException erro) {
            HttpStatus status = erro.getStatusCode().value() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status, "Saldo recusou a solicitacao: conta invalida ou valor invalido.");
        } catch (ResourceAccessException erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servico Saldo indisponivel.");
        }
    }
}
