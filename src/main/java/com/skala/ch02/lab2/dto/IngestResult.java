package com.skala.ch02.lab2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record IngestResult(
        @Schema(example = "return-policy") String source,
        @Schema(example = "7") int chunks) {
}
