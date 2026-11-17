package br.com.banco.saldo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/contas")
public class ContaController {

    /** JSON de POST /contas. */
    public record NovaConta(String numero, BigDecimal saldoInicial) { }

    /** JSON de POST /contas/{id}/debitos (chamado pelos outros microsservicos). */
    public record NovoDebito(BigDecimal valor) { }

    /** Resposta do debito: autorizado=false significa saldo insuficiente. */
    public record ResultadoDebito(boolean autorizado) { }

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping
    public Conta criar(@RequestBody NovaConta dados) {
        return service.criar(dados.numero(), dados.saldoInicial());
    }

    @GetMapping("/{id}")
    public Conta consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PostMapping("/{id}/debitos")
    public ResultadoDebito debitar(@PathVariable Long id, @RequestBody NovoDebito dados) {
        return new ResultadoDebito(service.debitar(id, dados.valor()));
    }
}
