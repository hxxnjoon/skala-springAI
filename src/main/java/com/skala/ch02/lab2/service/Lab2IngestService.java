package com.skala.ch02.lab2.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.skala.ch02.lab2.dto.IngestResult;

@Service
public class Lab2IngestService {

    // lab2-docs/*.md 파일명 — golden.json 의 src 값과 이 이름을 맞춰 둔다
    private static final List<String> SAMPLE_SOURCES = List.of("return-policy", "shipping-policy", "membership");
    private static final String SAMPLE_VERSION = "2026-07";

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public Lab2IngestService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    public List<IngestResult> ingestSampleDocs() {
        return SAMPLE_SOURCES.stream()
                .map(source -> ingest(
                        resourceLoader.getResource("classpath:lab2-docs/" + source + ".md"),
                        source, SAMPLE_VERSION))
                .toList();
    }

    public IngestResult ingest(Resource doc, String source, String version) {
        return ingest(doc, source, version, 200, 100); // "## N. 섹션" 하나(표 포함)가 한 청크에 담기는 크기
    }

    public IngestResult ingest(Resource doc, String source, String version, int chunkSize, int minChunkSizeChars) {
        var reader = new TextReader(doc); // .md → Document
        reader.getCustomMetadata().put("source", source); // 나중에 못 넣는다
        reader.getCustomMetadata().put("version", version);

        // TextReader.get() 은 "source" 메타데이터를 실제 리소스 파일명으로 덮어쓴다
        // (예: "return-policy" → "return-policy.md") — 아래서 그 실제 값을 다시 읽어
        // delete 필터에 그대로 써야 재인덱싱 때 기존 청크가 실제로 지워진다.
        List<Document> raw = reader.get();
        String actualSource = String.valueOf(raw.get(0).getMetadata().get("source"));

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(minChunkSizeChars)
                .build();
        List<Document> chunks = splitter.apply(raw);

        vectorStore.delete(new FilterExpressionBuilder() // 재인덱싱 —
                .eq("source", actualSource).build()); // 같은 출처를 지우고
        vectorStore.add(chunks); // 다시 넣는다
        return new IngestResult(actualSource, chunks.size());
    }
}
