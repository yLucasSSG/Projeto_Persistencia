package br.com.banco.pagamentos.controller;

import br.com.banco.pagamentos.exception.OperacaoException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;

@RestControllerAdvice
public class TratadorDeErros {
    public record ErroResponse(String mensagem, List<String> detalhes) { }
    @ExceptionHandler(OperacaoException.class)
    public ResponseEntity<ErroResponse> operacao(OperacaoException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErroResponse(exception.getMessage(), List.of()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException exception) {
        var detalhes = exception.getBindingResult().getAllErrors().stream()
                .map(erro -> erro.getDefaultMessage()).sorted().toList();
        return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos.", detalhes));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> formato() {
        return ResponseEntity.badRequest().body(new ErroResponse("Confira o JSON, a modalidade e os identificadores.", List.of()));
    }
}
