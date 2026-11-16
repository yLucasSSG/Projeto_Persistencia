package br.com.banco.saldo.controller;

import br.com.banco.saldo.dto.DebitoRequest;
import br.com.banco.saldo.dto.DebitoResponse;
import br.com.banco.saldo.service.DebitoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contas/{contaId}/debitos")
public class DebitoController {

    private final DebitoService service;

    public DebitoController(DebitoService service) {
        this.service = service;
    }

    @PostMapping
    public DebitoResponse debitar(@PathVariable Long contaId, @Valid @RequestBody DebitoRequest dados) {
        return service.debitar(contaId, dados);
    }
}
