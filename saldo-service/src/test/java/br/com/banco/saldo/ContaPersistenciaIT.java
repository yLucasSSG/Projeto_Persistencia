package br.com.banco.saldo;

import br.com.banco.saldo.dto.ContaResponse;
import br.com.banco.saldo.dto.ErroResponse;
import br.com.banco.saldo.dto.DebitoResponse;
import br.com.banco.saldo.model.ResultadoDebito;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaPersistenciaIT {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("banco_saldo_teste");

    private ConfigurableApplicationContext application;
    private RestClient api;
    private JdbcTemplate jdbc;

    @BeforeAll
    static void iniciarMySql() {
        MYSQL.start();
    }

    @AfterAll
    static void encerrarMySql() {
        MYSQL.stop();
    }

    @BeforeEach
    void iniciarAplicacao() {
        application = new SpringApplicationBuilder(SaldoServiceApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "--spring.datasource.username=" + MYSQL.getUsername(),
                "--spring.datasource.password=" + MYSQL.getPassword(),
                "--spring.jpa.hibernate.ddl-auto=update"
        );
        String port = application.getEnvironment().getProperty("local.server.port");
        api = RestClient.create("http://127.0.0.1:" + port);
        jdbc = application.getBean(JdbcTemplate.class);
    }

    @AfterEach
    void encerrarAplicacao() {
        if (application != null) {
            jdbc.update("DELETE FROM debitos");
            jdbc.update("DELETE FROM contas");
            application.close();
        }
    }

    @Test
    void gravaNoMySqlEConsultaDepoisDeReiniciarAplicacao() {
        ResponseEntity<ContaResponse> resposta = criar("1001", new BigDecimal("150.25"));
        assertEquals(201, resposta.getStatusCode().value());
        ContaResponse criada = resposta.getBody();
        assertNotNull(criada);
        assertNotNull(criada.id());
        assertEquals("/contas/" + criada.id(), resposta.getHeaders().getLocation().toString());
        assertEquals(0, jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, criada.id())
                .compareTo(new BigDecimal("150.25")));

        // Fecha o contexto, o servidor HTTP e as conexões JPA; mantém somente o MySQL.
        application.close();
        iniciarAplicacao();

        ContaResponse consultada = api.get().uri("/contas/{id}", criada.id())
                .retrieve().body(ContaResponse.class);
        assertNotNull(consultada);
        assertEquals(criada.id(), consultada.id());
        assertEquals("1001", consultada.numero());
        assertEquals(0, new BigDecimal("150.25").compareTo(consultada.saldo()));
    }

    @Test
    void rejeitaDadosInvalidosSemGravarRegistros() {
        List<String> invalidos = List.of(
                "{}",
                "null",
                "{\"numero\":\"   \",\"saldoInicial\":0}",
                "{\"numero\":\"1002\",\"saldoInicial\":-0.01}",
                "{\"numero\":\"1002\",\"saldoInicial\":1.001}",
                "{\"numero\":\"1002\",\"saldoInicial\":100000000000000000.00}",
                "{\"numero\":\"1234567890123456789012345678901\",\"saldoInicial\":0}",
                "{\"numero\":\"1002\",\"saldoInicial\":\"abc\"}",
                "{"
        );

        for (String json : invalidos) {
            RestClientResponseException erro = assertThrows(RestClientResponseException.class,
                    () -> api.post().uri("/contas").contentType(MediaType.APPLICATION_JSON)
                            .body(json).retrieve().toBodilessEntity());
            assertEquals(400, erro.getStatusCode().value(), json);
            ErroResponse corpo = erro.getResponseBodyAs(ErroResponse.class);
            assertNotNull(corpo);
            assertNotNull(corpo.mensagem());
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM contas", Long.class));
    }

    @Test
    void aceitaSaldoZeroEImpedeNumeroRepetidoSemAlterarContaExistente() {
        ContaResponse criada = criar("2001", BigDecimal.ZERO).getBody();
        assertNotNull(criada);

        RestClientResponseException erro = assertThrows(RestClientResponseException.class,
                () -> criar(" 2001 ", new BigDecimal("999.99")));
        assertEquals(409, erro.getStatusCode().value());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM contas", Long.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, criada.id())
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void diferenciaContaInexistenteDeIdentificadorInvalido() {
        RestClientResponseException inexistente = assertThrows(RestClientResponseException.class,
                () -> api.get().uri("/contas/9223372036854775807").retrieve().toBodilessEntity());
        assertEquals(404, inexistente.getStatusCode().value());

        RestClientResponseException invalido = assertThrows(RestClientResponseException.class,
                () -> api.get().uri("/contas/abc").retrieve().toBodilessEntity());
        assertEquals(400, invalido.getStatusCode().value());
    }

    @Test
    void permiteSomenteUmaContaQuandoCadastrosSaoSimultaneos() {
        CompletableFuture<Integer> primeira = CompletableFuture.supplyAsync(() -> tentarCriar("3001"));
        CompletableFuture<Integer> segunda = CompletableFuture.supplyAsync(() -> tentarCriar("3001"));

        List<Integer> status = List.of(primeira.join(), segunda.join()).stream().sorted().toList();
        assertEquals(List.of(201, 409), status);
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM contas", Long.class));
    }

    private ResponseEntity<ContaResponse> criar(String numero, BigDecimal saldoInicial) {
        return api.post().uri("/contas").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("numero", numero, "saldoInicial", saldoInicial))
                .retrieve().toEntity(ContaResponse.class);
    }

    private int tentarCriar(String numero) {
        try {
            return criar(numero, BigDecimal.ZERO).getStatusCode().value();
        } catch (RestClientResponseException exception) {
            assertTrue(exception.getStatusCode().is4xxClientError());
            return exception.getStatusCode().value();
        }
    }

    @Test
    void debitaUmaVezMesmoDepoisDeReiniciarAplicacao() {
        Long contaId = criar("4001", new BigDecimal("100.00")).getBody().id();
        UUID id = UUID.randomUUID();
        assertEquals(ResultadoDebito.AUTORIZADO, debitar(contaId, id, "30.25").resultado());
        application.close();
        iniciarAplicacao();
        assertEquals(ResultadoDebito.AUTORIZADO, debitar(contaId, id, "30.25").resultado());
        assertEquals(0, new BigDecimal("69.75").compareTo(saldo(contaId)));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM debitos", Long.class));
    }

    @Test
    void saldoInsuficienteNaoAlteraContaEPermaneceRegistrado() {
        Long contaId = criar("4002", new BigDecimal("10.00")).getBody().id();
        UUID id = UUID.randomUUID();
        assertEquals(ResultadoDebito.SALDO_INSUFICIENTE, debitar(contaId, id, "20.00").resultado());
        assertEquals(ResultadoDebito.SALDO_INSUFICIENTE, debitar(contaId, id, "20.00").resultado());
        assertEquals(0, new BigDecimal("10.00").compareTo(saldo(contaId)));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM debitos", Long.class));
    }

    @Test
    void debitosSimultaneosNaoGastamOMesmoSaldo() {
        Long contaId = criar("4003", new BigDecimal("100.00")).getBody().id();
        var primeira = CompletableFuture.supplyAsync(() -> debitar(contaId, UUID.randomUUID(), "80.00"));
        var segunda = CompletableFuture.supplyAsync(() -> debitar(contaId, UUID.randomUUID(), "80.00"));
        List<ResultadoDebito> resultados = List.of(primeira.join().resultado(), segunda.join().resultado());
        assertTrue(resultados.contains(ResultadoDebito.AUTORIZADO));
        assertTrue(resultados.contains(ResultadoDebito.SALDO_INSUFICIENTE));
        assertEquals(0, new BigDecimal("20.00").compareTo(saldo(contaId)));
    }

    @Test
    void repeticoesSimultaneasDaMesmaTransacaoDescontamUmaVez() {
        Long contaId = criar("4004", new BigDecimal("100.00")).getBody().id();
        UUID id = UUID.randomUUID();
        var primeira = CompletableFuture.supplyAsync(() -> debitar(contaId, id, "40.00"));
        var segunda = CompletableFuture.supplyAsync(() -> debitar(contaId, id, "40.00"));
        assertEquals(primeira.join(), segunda.join());
        assertEquals(0, new BigDecimal("60.00").compareTo(saldo(contaId)));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM debitos", Long.class));
    }

    @Test
    void idReutilizadoComOutroValorRetornaConflito() {
        Long contaId = criar("4005", new BigDecimal("100.00")).getBody().id();
        UUID id = UUID.randomUUID();
        debitar(contaId, id, "20.00");
        RestClientResponseException erro = assertThrows(RestClientResponseException.class,
                () -> debitar(contaId, id, "21.00"));
        assertEquals(409, erro.getStatusCode().value());
        assertEquals(0, new BigDecimal("80.00").compareTo(saldo(contaId)));
    }

    @Test
    void mesmaTransacaoEmDuasContasReverteODebitoConflitante() {
        Long primeiraConta = criar("4006", new BigDecimal("100.00")).getBody().id();
        Long segundaConta = criar("4007", new BigDecimal("100.00")).getBody().id();
        UUID id = UUID.randomUUID();
        var primeira = CompletableFuture.supplyAsync(() -> tentarDebitar(primeiraConta, id));
        var segunda = CompletableFuture.supplyAsync(() -> tentarDebitar(segundaConta, id));
        assertEquals(List.of(200, 409), List.of(primeira.join(), segunda.join()).stream().sorted().toList());
        assertEquals(0, new BigDecimal("120.00").compareTo(saldo(primeiraConta).add(saldo(segundaConta))));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM debitos", Long.class));
    }

    private DebitoResponse debitar(Long contaId, UUID id, String valor) {
        return api.post().uri("/contas/{id}/debitos", contaId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("idTransacao", id, "valor", new BigDecimal(valor), "referencia", "TESTE"))
                .retrieve().body(DebitoResponse.class);
    }

    private BigDecimal saldo(Long contaId) {
        return api.get().uri("/contas/{id}", contaId).retrieve().body(ContaResponse.class).saldo();
    }

    private int tentarDebitar(Long contaId, UUID id) {
        try {
            debitar(contaId, id, "80.00");
            return 200;
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode().value();
        }
    }
}
