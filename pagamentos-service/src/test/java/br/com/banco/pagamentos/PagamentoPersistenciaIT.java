package br.com.banco.pagamentos;

import br.com.banco.pagamentos.dto.PagamentoResponse;
import br.com.banco.pagamentos.model.SituacaoOperacao;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoPersistenciaIT {
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    private static final AtomicInteger CHAMADAS = new AtomicInteger();
    private static final AtomicBoolean INDISPONIVEL = new AtomicBoolean();
    private static HttpServer saldo;
    private ConfigurableApplicationContext application;
    private RestClient api;
    private JdbcTemplate jdbc;

    @BeforeAll
    static void preparar() throws Exception {
        MYSQL.start();
        saldo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        saldo.createContext("/contas/", exchange -> {
            CHAMADAS.incrementAndGet();
            if (INDISPONIVEL.get()) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            var json = JsonMapper.builder().build();
            DebitoRequest dados = json.readValue(exchange.getRequestBody().readAllBytes(), DebitoRequest.class);
            long contaId = Long.parseLong(exchange.getRequestURI().getPath().split("/")[2]);
            byte[] resposta = json.writeValueAsBytes(Map.of("idTransacao", dados.idTransacao(), "contaId", contaId,
                    "valor", dados.valor(), "resultado", contaId == 2 ? "SALDO_INSUFICIENTE" : "AUTORIZADO"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resposta.length);
            exchange.getResponseBody().write(resposta);
            exchange.close();
        });
        saldo.start();
    }

    @AfterAll
    static void finalizar() {
        if (saldo != null) saldo.stop(0);
        MYSQL.stop();
    }

    @BeforeEach
    void iniciar() {
        CHAMADAS.set(0);
        INDISPONIVEL.set(false);
        abrirAplicacao();
    }

    private void abrirAplicacao() {
        application = new SpringApplicationBuilder(PagamentoApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword(),
                "--saldo.url=http://127.0.0.1:" + saldo.getAddress().getPort());
        api = RestClient.create("http://127.0.0.1:" + application.getEnvironment().getProperty("local.server.port"));
        jdbc = application.getBean(JdbcTemplate.class);
    }

    @AfterEach
    void fechar() {
        if (application != null) {
            jdbc.update("DELETE FROM comunicacoes_banco_central");
            jdbc.update("DELETE FROM pagamentos");
            application.close();
        }
    }

    @Test
    void persisteTodasAsModalidadesEConsultaAposReiniciar() {
        UUID primeira = null;
        for (String modalidade : List.of("PIX", "BOLETO", "SALDO_EM_CONTA")) {
            UUID id = UUID.randomUUID();
            if (primeira == null) primeira = id;
            var resposta = criar(dados(id, 1, modalidade));
            assertEquals(SituacaoOperacao.CONCLUIDA, resposta.situacao());
            assertEquals("SIM-" + id, resposta.protocoloBancoCentral());
        }
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos", Long.class));
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM comunicacoes_banco_central", Long.class));
        application.close();
        abrirAplicacao();
        assertEquals(SituacaoOperacao.CONCLUIDA, consultar(primeira).situacao());
        assertEquals("SIM-" + primeira, consultar(primeira).protocoloBancoCentral());
    }

    @Test
    void registraSaldoInsuficienteSemComunicarOperacaoNaoEfetivada() {
        var resposta = criar(dados(UUID.randomUUID(), 2, "PIX"));
        assertEquals(SituacaoOperacao.SALDO_INSUFICIENTE, resposta.situacao());
        assertNull(resposta.protocoloBancoCentral());
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM comunicacoes_banco_central", Long.class));
    }

    @Test
    void reenvioNaoRepeteDebitoNemComunicacaoEConflitoNaoSobrescreveOrdem() {
        UUID id = UUID.randomUUID();
        Map<String, Object> dados = dados(id, 1, "PIX");
        criar(dados);
        criar(dados);
        assertEquals(1, CHAMADAS.get());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM comunicacoes_banco_central", Long.class));
        dados.put("valor", new BigDecimal("99.00"));
        var erro = assertThrows(RestClientResponseException.class, () -> criar(dados));
        assertEquals(409, erro.getStatusCode().value());
        assertEquals(0, new BigDecimal("10.00").compareTo(consultar(id).valor()));
    }

    @Test
    void falhaDeSaldoMantemOrdemPendenteParaRetomar() {
        UUID id = UUID.randomUUID();
        var dados = dados(id, 1, "PIX");
        INDISPONIVEL.set(true);
        var erro = assertThrows(RestClientResponseException.class, () -> criar(dados));
        assertEquals(503, erro.getStatusCode().value());
        assertEquals(SituacaoOperacao.PENDENTE, consultar(id).situacao());
        INDISPONIVEL.set(false);
        assertEquals(SituacaoOperacao.CONCLUIDA, criar(dados).situacao());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM comunicacoes_banco_central", Long.class));
    }

    @Test
    void solicitacoesSimultaneasDaMesmaOrdemProcessamUmaVez() {
        var dados = dados(UUID.randomUUID(), 1, "PIX");
        var primeira = CompletableFuture.supplyAsync(() -> criar(dados));
        var segunda = CompletableFuture.supplyAsync(() -> criar(dados));
        assertEquals(primeira.join(), segunda.join());
        assertEquals(1, CHAMADAS.get());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos", Long.class));
    }

    @Test
    void dadosInvalidosNaoGeramOrdens() {
        for (String json : List.of("{}", "null", "{", "{\"idTransacao\":\"00000000-0000-0000-0000-000000000001\",\"contaId\":1,\"valor\":-1,\"modalidade\":\"PIX\"}")) {
            var erro = assertThrows(RestClientResponseException.class, () -> api.post().uri("/pagamentos")
                    .contentType(MediaType.APPLICATION_JSON).body(json).retrieve().toBodilessEntity());
            assertEquals(400, erro.getStatusCode().value());
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos", Long.class));
        assertEquals(0, CHAMADAS.get());
    }

    private Map<String, Object> dados(UUID id, long conta, String modalidade) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("idTransacao", id);
        dados.put("contaId", conta);
        dados.put("valor", new BigDecimal("10.00"));
        dados.put("modalidade", modalidade);
        dados.put(modalidade.equals("BOLETO") ? "codigoBarras" : "destino", "destino-teste");
        return dados;
    }

    @Test
    void exigeCodigoDeBarrasParaBoletoEDestinoParaOutrasModalidades() {
        var boletoComDestino = dados(UUID.randomUUID(), 1, "BOLETO");
        boletoComDestino.put("destino", "destino-indevido");
        var pixSemDestino = dados(UUID.randomUUID(), 1, "PIX");
        pixSemDestino.remove("destino");
        pixSemDestino.put("codigoBarras", "codigo-indevido");
        var saldoSemDestino = dados(UUID.randomUUID(), 1, "SALDO_EM_CONTA");
        saldoSemDestino.put("destino", "   ");

        for (var invalido : List.of(boletoComDestino, pixSemDestino, saldoSemDestino)) {
            var erro = assertThrows(RestClientResponseException.class, () -> criar(invalido));
            assertEquals(400, erro.getStatusCode().value());
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos", Long.class));
        assertEquals(0, CHAMADAS.get());
    }
    private PagamentoResponse criar(Map<String, Object> dados) {
        return api.post().uri("/pagamentos").contentType(MediaType.APPLICATION_JSON).body(dados)
                .retrieve().body(PagamentoResponse.class);
    }
    private PagamentoResponse consultar(UUID id) {
        return api.get().uri("/pagamentos/{id}", id).retrieve().body(PagamentoResponse.class);
    }
    public record DebitoRequest(UUID idTransacao, BigDecimal valor, String referencia) { }
}
