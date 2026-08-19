package com.skala.ch02.lab2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(example = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.") String message,
        @Schema(example = "a1b2c3d4") String traceId) {
}
