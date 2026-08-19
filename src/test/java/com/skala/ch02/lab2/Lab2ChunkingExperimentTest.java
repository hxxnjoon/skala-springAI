package com.skala.ch02.lab2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.ch02.lab2.dto.AnswerDto;
import com.skala.ch02.lab2.service.Lab2IngestService;
import com.skala.ch02.lab2.service.Lab2RagService;

/**
 * Day2 실습 Step5 — 청킹·top-k·threshold·겹침 조합별 통과율 실험표.
 * 한 번에 하나만 바꿔가며 골든셋 10문항을 다시 채점한다. 실제 모델을 6번(조합 수) 반복
 * 호출하므로 {@code @Tag("eval")} 로 기본 {@code ./gradlew test} 에서는 제외되고
 * {@code ./gradlew test -Peval} 로만 실행된다.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
class Lab2ChunkingExperimentTest {

    private static final Logger log = LoggerFactory.getLogger(Lab2ChunkingExperimentTest.class);
    private static final List<String> SAMPLE_SOURCES = List.of("return-policy", "shipping-policy", "membership");
    private static final String VERSION = "2026-07";

    @Autowired
    private Lab2IngestService ingestService;
    @Autowired
    private Lab2RagService ragService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private ResourceLoader resourceLoader;

    private List<Golden> golden;

    @BeforeAll
    void loadGolden() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("lab2/golden.json").getInputStream()) {
            golden = mapper.readValue(in, new TypeReference<List<Golden>>() {
            });
        }
    }

    @Test
    @DisplayName("Step5 — 청크·top-k·threshold·겹침 조합별 실험표")
    void 실험표를_채운다() {
        List<Combo> combos = List.of(
                new Combo("A(기준)  400토큰·겹침0", 400, 200, 4, 0.4, false),
                new Combo("B(작게)  200토큰", 200, 100, 4, 0.4, false),
                new Combo("C(크게)  800토큰", 800, 400, 4, 0.4, false),
                new Combo("D(넓게)  400토큰·top-k8", 400, 200, 8, 0.4, false),
                new Combo("E(엄격)  400토큰·threshold0.7", 400, 200, 4, 0.7, false),
                new Combo("F(겹침)  400토큰 근사·겹침20%", 400, 200, 4, 0.4, true));

        List<String> rows = new ArrayList<>();
        for (Combo c : combos) {
            log.info(">>> {} 인제스트 중...", c.name());
            ingestForCombo(c);
            int pass = evaluate(c);
            String row = "%-32s | 청크 %4d · top-k %d · threshold %.1f%s | %d/10"
                    .formatted(c.name(), c.chunkSize(), c.topK(), c.threshold(), c.overlap() ? " · 겹침20%" : "", pass);
            rows.add(row);
            log.info("=== {} ===", row);
        }

        log.info("\nStep5 실험표\n{}", String.join("\n", rows));

        // 실험이 끝나면 최종 채택값(200/100, top-k4, threshold0.4)으로 되돌려 둔다
        ingestService.ingestSampleDocs();
    }

    private void ingestForCombo(Combo c) {
        for (String source : SAMPLE_SOURCES) {
            Resource resource = resourceLoader.getResource("classpath:lab2-docs/" + source + ".md");
            if (c.overlap()) {
                ingestWithOverlap(resource, source, c.chunkSize());
            } else {
                ingestService.ingest(resource, source, VERSION, c.chunkSize(), c.minChunkSizeChars());
            }
        }
    }

    // TokenTextSplitter 는 겹침(overlap) 옵션이 없어(Spring AI 1.1.8 확인) 여기서만 직접 구현한다.
    private void ingestWithOverlap(Resource resource, String source, int chunkSizeTokens) {
        var reader = new TextReader(resource);
        String text = reader.get().get(0).getText();

        int chunkChars = (int) (chunkSizeTokens * 1.5); // 한국어 토큰당 대략 1.5자 근사
        int stride = Math.max(1, (int) Math.round(chunkChars * 0.8)); // 20% 겹침

        List<Document> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += stride) {
            int end = Math.min(start + chunkChars, text.length());
            Map<String, Object> meta = new HashMap<>();
            meta.put("source", source);
            meta.put("version", VERSION);
            chunks.add(new Document(text.substring(start, end), meta));
            if (end == text.length()) {
                break;
            }
        }

        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
        vectorStore.add(chunks);
    }

    private int evaluate(Combo c) {
        int pass = 0;
        for (Golden g : golden) {
            AnswerDto a = ragService.ask(g.q(), c.topK(), c.threshold());
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            boolean cite = g.src() == null || a.sources().stream().anyMatch(s -> s.contains(g.src()));
            if (hit && cite) {
                pass++;
            } else {
                log.warn("  [{}] 실패: {}\n    답변: {}\n    출처: {}", c.name(), g.q(), a.answer(), a.sources());
            }
        }
        return pass;
    }

    private record Combo(String name, int chunkSize, int minChunkSizeChars, int topK, double threshold,
            boolean overlap) {
    }

    private record Golden(String q, List<String> must, String src) {
    }
}
