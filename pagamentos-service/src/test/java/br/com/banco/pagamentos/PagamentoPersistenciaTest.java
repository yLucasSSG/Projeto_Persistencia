package br.com.banco.pagamentos;

import br.com.banco.pagamentos.bancocentral.ComunicacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Testa os dois caminhos do pagamento. O servico Saldo e simulado (mock) e o banco e o H2 em memoria. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-pagamentos",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class PagamentoPersistenciaTest {

    @MockitoBean SaldoClient saldo;

    @Autowired PagamentoService service;
    @Autowired PagamentoRepository repository;
    @Autowired ComunicacaoRepository comunicacoes;

    @Test
    void boletoComSaldoConcluiGravaEComunicaBancoCentral() {
        when(saldo.debitar(any(), any())).thenReturn(true);
        long antes = comunicacoes.count();

        Pagamento p = service.pagar(1L, "34191.79001 01043.510047", new BigDecimal("120.50"), ModalidadePagamento.BOLETO);

        Pagamento doBanco = repository.findById(p.getId()).get();
        assertEquals(SituacaoOperacao.CONCLUIDA, doBanco.getSituacao());
        assertNotNull(doBanco.getProtocoloBancoCentral());
        assertEquals(antes + 1, comunicacoes.count());
    }

    @Test
    void saldoEmContaSemSaldoGravaComoInsuficienteESemComunicar() {
        when(saldo.debitar(any(), any())).thenReturn(false);
        long antes = comunicacoes.count();

        Pagamento p = service.pagar(1L, "conta-destino", new BigDecimal("20.00"), ModalidadePagamento.SALDO_EM_CONTA);

        Pagamento doBanco = repository.findById(p.getId()).get();
        assertEquals(SituacaoOperacao.SALDO_INSUFICIENTE, doBanco.getSituacao());
        assertNull(doBanco.getProtocoloBancoCentral());
        assertEquals(antes, comunicacoes.count());
    }
}
