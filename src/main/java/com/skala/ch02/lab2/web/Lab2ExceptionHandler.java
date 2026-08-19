package com.skala.ch02.lab2.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.ch02.lab2.dto.ErrorResponse;

@RestControllerAdvice(basePackages = "com.skala.ch02.lab2.web")
class Lab2ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Lab2ExceptionHandler.class);

    @ExceptionHandler(Exception.class)          // 인제스트·검색·모델 호출 실패 전부 포함
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] RAG 처리 실패", traceId, e);   // 상세는 로그에만
        return ResponseEntity.status(503).body(new ErrorResponse(
                "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", traceId));
    }
}
