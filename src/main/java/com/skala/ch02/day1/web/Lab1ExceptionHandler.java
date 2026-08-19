package com.skala.ch02.day1.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.ch02.day1.dto.ErrorResponse;
import com.skala.ch02.service.OrderNotFoundException;

// ① 예외 → 응답 변환은 한 곳에서 (1장)
@RestControllerAdvice
class Lab1ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Lab1ExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(OrderNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse("주문을 찾을 수 없습니다.", null));
    }

    @ExceptionHandler(Exception.class)          // 모델 오류·타임아웃 포함
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 요약 실패", traceId, e);   // 상세는 로그에만
        return ResponseEntity.status(503).body(new ErrorResponse(
                "요약을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.", traceId));
    }
}
