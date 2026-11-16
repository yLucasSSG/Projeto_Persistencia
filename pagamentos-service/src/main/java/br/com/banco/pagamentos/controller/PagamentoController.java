package br.com.banco.pagamentos.controller;

import br.com.banco.pagamentos.dto.*;
import br.com.banco.pagamentos.service.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {
    private final PagamentoService service;
    public PagamentoController(PagamentoService service) { this.service = service; }
    @PostMapping
    public PagamentoResponse criar(@Valid @RequestBody CriarPagamentoRequest dados) { return service.criar(dados); }
    @GetMapping("/{id}")
    public PagamentoResponse consultar(@PathVariable UUID id) { return service.consultar(id); }
}
