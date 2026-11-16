package br.com.banco.pagamentos.service;

import br.com.banco.pagamentos.exception.OperacaoException;
import br.com.banco.pagamentos.model.Pagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class SaldoClient {
    private final RestClient client;
    public SaldoClient(@Value("${saldo.url}") String url) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public boolean debitar(Pagamento ordem) {
        try {
            DebitoResponse resposta = client.post().uri("/contas/{id}/debitos", ordem.getContaId())
                    .body(Map.of("idTransacao", ordem.getIdTransacao(), "valor", ordem.getValor(),
                            "referencia", ordem.referenciaDebito()))
                    .retrieve().body(DebitoResponse.class);
            if (resposta == null || !ordem.getIdTransacao().equals(resposta.idTransacao())
                    || !ordem.getContaId().equals(resposta.contaId()) || resposta.valor() == null
                    || ordem.getValor().compareTo(resposta.valor()) != 0
                    || !("AUTORIZADO".equals(resposta.resultado()) || "SALDO_INSUFICIENTE".equals(resposta.resultado()))) {
                throw new OperacaoException(503, "Resposta inválida de Saldo; a ordem permanece pendente.");
            }
            return "AUTORIZADO".equals(resposta.resultado());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 409 || status == 400) {
                throw new OperacaoException(status, "Saldo recusou a solicitação. Confira a conta e o identificador da transação.");
            }
            throw indisponivel();
        } catch (RestClientException exception) {
            throw indisponivel();
        }
    }
    private OperacaoException indisponivel() {
        return new OperacaoException(503, "Saldo indisponível; a ordem permanece pendente. Reenvie a mesma solicitação.");
    }
    private record DebitoResponse(UUID idTransacao, Long contaId, BigDecimal valor, String resultado) { }
}
