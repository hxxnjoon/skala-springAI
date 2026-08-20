package com.skala.ch02.lab3.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record Lab3ChatResponse(
        @Schema(example = "단순 변심 반품은 상품 수령일로부터 7일 이내입니다. [출처: return-policy]") String answer,
        @Schema(description = "규정 문서를 근거로 답했을 때만 채워진다", example = "[\"return-policy.md\"]") List<String> sources,
        @Schema(description = "규정 문서 근거로 답했는지 여부 — 도구 호출/일반 대화는 false") boolean grounded) {
}
