package com.aluguelcarros.aluguel.api;

import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,Object>> business(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"Operação inválida.")));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f->fields.put(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message","Revise os campos informados.","fields",fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
    ResponseEntity<Map<String,String>> malformed(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message","Dados inválidos ou incompletos. Confira os campos e tente novamente."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> duplicate(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message","Há dados duplicados ou inconsistentes. Confira CPF, usuário, matrícula e placa."));
    }
    @ExceptionHandler({OptimisticLockingFailureException.class,PessimisticLockingFailureException.class})
    ResponseEntity<Map<String,String>> concurrent(Exception e) {
        return ResponseEntity.status(409).body(Map.of("message","Os dados mudaram durante a operação. Atualize e tente novamente."));
    }
}
