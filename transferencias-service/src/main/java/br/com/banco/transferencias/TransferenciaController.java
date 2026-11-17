package br.com.banco.transferencias;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    /** Dados que chegam no JSON de POST /transferencias. */
    public record NovaTransferencia(Long contaId, String destino, BigDecimal valor, ModalidadeTransferencia modalidade) { }

    private final TransferenciaService service;

    public TransferenciaController(TransferenciaService service) {
        this.service = service;
    }

    @PostMapping
    public Transferencia transferir(@RequestBody NovaTransferencia dados) {
        return service.transferir(dados.contaId(), dados.destino(), dados.valor(), dados.modalidade());
    }

    @GetMapping("/{id}")
    public Transferencia consultar(@PathVariable Long id) {
        return service.consultar(id);
    }
}
