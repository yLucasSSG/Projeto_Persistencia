package br.com.banco.transferencias.controller;

import br.com.banco.transferencias.dto.*;
import br.com.banco.transferencias.service.TransferenciaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {
    private final TransferenciaService service;
    public TransferenciaController(TransferenciaService service) { this.service = service; }
    @PostMapping
    public TransferenciaResponse criar(@Valid @RequestBody CriarTransferenciaRequest dados) { return service.criar(dados); }
    @GetMapping("/{id}")
    public TransferenciaResponse consultar(@PathVariable UUID id) { return service.consultar(id); }
}
