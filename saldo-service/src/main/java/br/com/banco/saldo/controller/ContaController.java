package br.com.banco.saldo.controller;

import br.com.banco.saldo.dto.ContaResponse;
import br.com.banco.saldo.dto.CriarContaRequest;
import br.com.banco.saldo.service.ContaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest dados) {
        ContaResponse conta = service.criar(dados);
        return ResponseEntity.created(URI.create("/contas/" + conta.id())).body(conta);
    }

    @GetMapping("/{id}")
    public ContaResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }
}
