package com.skala.ch02.day1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SummaryResponse(
        @Schema(example = "12345") String orderId,
        @Schema(example = "무선 이어폰이 배송 중이며 7월 30일 도착 예정입니다.") String summary) {
}
