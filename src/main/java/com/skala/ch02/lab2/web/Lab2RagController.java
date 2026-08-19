package com.skala.ch02.lab2.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.ch02.lab2.dto.AnswerDto;
import com.skala.ch02.lab2.dto.ChunkResponse;
import com.skala.ch02.lab2.dto.IngestResult;
import com.skala.ch02.lab2.service.Lab2IngestService;
import com.skala.ch02.lab2.service.Lab2RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab2")
@Tag(name = "Day2 실습 · 문서 Q&A(RAG)")
public class Lab2RagController {

    private final Lab2IngestService ingestService;
    private final Lab2RagService ragService;

    public Lab2RagController(Lab2IngestService ingestService, Lab2RagService ragService) {
        this.ingestService = ingestService;
        this.ragService = ragService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "샘플 정책 문서 인제스트",
                description = "lab2-docs 의 배송·반품·멤버십 규정을 벡터 스토어에 넣는다. "
                        + "재실행해도 같은 출처는 지우고 다시 넣어 중복이 쌓이지 않는다.")
    public List<IngestResult> ingest() {
        return ingestService.ingestSampleDocs();
    }

    @GetMapping("/retrieve")
    @Operation(summary = "근거 문서 검색", description = "답변을 만들기 전에 검색 결과와 점수를 그대로 보여준다.")
    public List<ChunkResponse> retrieve(
            @Parameter(description = "질문", example = "제주도 배송비") @RequestParam String q,
            @Parameter(description = "상위 K 개") @RequestParam(defaultValue = "4") int topK) {
        return ragService.retrieve(q, topK).stream()
                .map(d -> new ChunkResponse(
                        String.valueOf(d.getMetadata().get("source")),
                        Lab2RagService.toDisplayScore(d.getScore()),
                        snippet(d.getText(), 120)))
                .toList();
    }

    @GetMapping("/ask")
    @Operation(summary = "문서 근거 질의응답",
                description = "검색된 근거 안에서만 답한다. 근거가 없으면 추측하지 않고 \"확인되지 않습니다\"라고 답한다.")
    public AnswerDto ask(
            @Parameter(description = "질문", example = "단순 변심 반품은 며칠 이내인가요") @RequestParam String q) {
        return ragService.ask(q);
    }

    private String snippet(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
