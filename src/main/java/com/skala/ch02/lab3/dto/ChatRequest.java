package com.skala.ch02.lab3.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRequest(
        @NotBlank @Schema(example = "user1") String userId,
        @NotBlank @Schema(description = "대화(세션) 식별자 — 같으면 이전 턴을 기억한다", example = "s1") String sessionId,
        @NotBlank @Schema(example = "단순 변심 반품은 며칠 이내인가요") String message) {
}
