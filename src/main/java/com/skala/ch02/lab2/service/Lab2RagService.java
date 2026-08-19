package com.skala.ch02.lab2.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.ch02.lab2.dto.AnswerDto;

@Service
public class Lab2RagService {

    private final VectorStore vectorStore;
    private final ChatClient ragChatClient;

    public Lab2RagService(VectorStore vectorStore, ChatClient ragChatClient) {
        this.vectorStore = vectorStore;
        this.ragChatClient = ragChatClient;
    }

    public List<Document> retrieve(String query, int topK) {
        return retrieve(query, topK, 0.5); // 과제 기준선
    }

    public List<Document> retrieve(String query, int topK, double threshold) {
        // SimpleVectorStore 는 원본 코사인 값을 그대로 준다(-1~1). 표시·threshold 판단은
        // (1+cos)/2 로 0~1 재스케일한 값을 기준으로 한다 — 흔히 보는 유사도 표기와 맞춘다.
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.0) // SearchRequest 는 [0,1] 만 허용 — 원본 값 자체는 여기서 거르지 않는다
                        .build())
                .stream()
                .filter(d -> toDisplayScore(d.getScore()) >= threshold)
                .toList();
    }

    public static double toDisplayScore(double rawCosineSimilarity) {
        return (1 + rawCosineSimilarity) / 2;
    }

    public AnswerDto ask(String question) {
        return ask(question, 4, 0.5);
    }

    public AnswerDto ask(String question, int topK, double threshold) {
        var docs = retrieve(question, topK, threshold);
        if (docs.isEmpty()) {
            return AnswerDto.unknown(); // 근거가 없으면 모델을 부르지 않는다
        }
        return ragChatClient.prompt()
                .system("""
                        아래 [근거]만 사용해 답한다. 근거에 없으면 "확인되지 않습니다"라고 답한다.
                        추측하지 않는다. 사용한 근거의 출처 파일명을 sources 목록에 모두 담는다.
                        답변 끝에도 사용한 출처를 [출처: 파일명] 형식으로 남긴다.
                        """)
                .user(u -> u.text("[근거]\n{context}\n\n[질문] {question}")
                        .param("context", format(docs))
                        .param("question", question))
                .call()
                .entity(AnswerDto.class);
    }

    private String format(List<Document> docs) {
        return docs.stream()
                .map(d -> "[%s] %s".formatted(d.getMetadata().get("source"), d.getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
