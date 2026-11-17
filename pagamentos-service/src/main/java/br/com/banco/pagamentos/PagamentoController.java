package br.com.banco.pagamentos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    /** Dados que chegam no JSON de POST /pagamentos. */
    public record NovoPagamento(Long contaId, String destino, BigDecimal valor, ModalidadePagamento modalidade) { }

    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @PostMapping
    public Pagamento pagar(@RequestBody NovoPagamento dados) {
        return service.pagar(dados.contaId(), dados.destino(), dados.valor(), dados.modalidade());
    }

    @GetMapping("/{id}")
    public Pagamento consultar(@PathVariable Long id) {
        return service.consultar(id);
    }
}
