package br.com.banco.transferencias;

import br.com.banco.transferencias.bancocentral.ComunicacaoRepository;
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

/** Testa os dois caminhos da transferencia. O servico Saldo e simulado (mock) e o banco e o H2 em memoria. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-transferencias",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class TransferenciaPersistenciaTest {

    @MockitoBean SaldoClient saldo;

    @Autowired TransferenciaService service;
    @Autowired TransferenciaRepository repository;
    @Autowired ComunicacaoRepository comunicacoes;

    @Test
    void comSaldoConcluiGravaEComunicaBancoCentral() {
        when(saldo.debitar(any(), any())).thenReturn(true);
        long antes = comunicacoes.count();

        Transferencia t = service.transferir(1L, "chave-destino", new BigDecimal("30.00"), ModalidadeTransferencia.PIX);

        Transferencia doBanco = repository.findById(t.getId()).get();
        assertEquals(SituacaoOperacao.CONCLUIDA, doBanco.getSituacao());
        assertNotNull(doBanco.getProtocoloBancoCentral());
        assertEquals(antes + 1, comunicacoes.count());
    }

    @Test
    void semSaldoGravaComoInsuficienteESemComunicar() {
        when(saldo.debitar(any(), any())).thenReturn(false);
        long antes = comunicacoes.count();

        Transferencia t = service.transferir(1L, "chave-destino", new BigDecimal("50.00"), ModalidadeTransferencia.TED);

        Transferencia doBanco = repository.findById(t.getId()).get();
        assertEquals(SituacaoOperacao.SALDO_INSUFICIENTE, doBanco.getSituacao());
        assertNull(doBanco.getProtocoloBancoCentral());
        assertEquals(antes, comunicacoes.count());
    }
}
