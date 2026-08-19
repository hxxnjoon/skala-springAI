package com.skala.ch02.web;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.ch02.service.OrderNotFoundException;

/**
 * 1장 — 예외를 응답으로 바꾸는 <b>한 곳</b>.
 *
 * <p>예외는 발생한 자리에서 그냥 던지고, HTTP 응답으로 옮기는 일은 여기서만 한다.
 * 컨트롤러마다 try-catch 를 넣으면 반드시 어딘가에서 형식이 어긋난다.
 *
 * <p>원칙 두 가지.
 * <ol>
 *   <li>내부 정보(스택트레이스·SQL·소유자 ID)를 절대 응답에 담지 않는다.</li>
 *   <li>없는 것과 권한 없는 것을 <b>구분하지 않는다</b> — 구분하면 존재 여부가 새어 나간다.</li>
 * </ol>
 */
@RestControllerAdvice
public class OrderExceptionHandler {

    /** 없는 주문과 남의 주문이 똑같이 404 다. */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "주문을 찾을 수 없습니다.",
                "timestamp", Instant.now().toString()));
    }

    /** {@code @Valid} 실패 — 어떤 필드가 왜 틀렸는지는 알려 줘도 안전하다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of(
                "message", detail.isBlank() ? "입력값이 올바르지 않습니다." : detail,
                "timestamp", Instant.now().toString()));
    }
}
