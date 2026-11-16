package br.com.banco.saldo.controller;

import br.com.banco.saldo.dto.ErroResponse;
import br.com.banco.saldo.exception.ContaJaExisteException;
import br.com.banco.saldo.exception.ContaNaoEncontradaException;
import br.com.banco.saldo.exception.TransacaoConflitanteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException exception) {
        List<String> detalhes = exception.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .toList();
        return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos.", detalhes));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido() {
        return ResponseEntity.badRequest().body(new ErroResponse(
                "Envie um JSON válido com os campos e tipos esperados para a operação.", List.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarIdentificadorInvalido() {
        return ResponseEntity.badRequest().body(new ErroResponse(
                "O identificador da conta deve ser um número inteiro válido.", List.of()));
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarContaNaoEncontrada(ContaNaoEncontradaException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }

    @ExceptionHandler(ContaJaExisteException.class)
    public ResponseEntity<ErroResponse> tratarContaJaExiste(ContaJaExisteException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }

    @ExceptionHandler(TransacaoConflitanteException.class)
    public ResponseEntity<ErroResponse> tratarTransacaoConflitante(TransacaoConflitanteException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }
}
