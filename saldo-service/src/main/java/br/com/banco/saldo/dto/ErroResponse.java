package br.com.banco.saldo.dto;

import java.util.List;

public record ErroResponse(String mensagem, List<String> detalhes) {
}
