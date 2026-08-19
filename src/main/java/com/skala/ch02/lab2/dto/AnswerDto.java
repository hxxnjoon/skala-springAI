package com.skala.ch02.lab2.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerDto(
        @Schema(example = "단순 변심은 상품 수령일로부터 7일 이내 반품할 수 있습니다. [출처: return-policy]") String answer,
        @Schema(example = "[\"return-policy\"]") List<String> sources,
        @Schema(example = "true") boolean grounded) {

    public static AnswerDto unknown() {
        return new AnswerDto("확인되지 않습니다.", List.of(), false);
    }
}
