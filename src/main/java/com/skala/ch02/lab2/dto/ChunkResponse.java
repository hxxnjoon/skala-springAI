package com.skala.ch02.lab2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChunkResponse(
        @Schema(example = "return-policy") String source,
        @Schema(example = "0.82") Double score,
        @Schema(example = "단순 변심은 상품 수령일로부터 7일 이내 반품할 수 있습니다...") String snippet) {
}
