package com.skala.ch02.lab3.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.ch02.lab3.dto.ChatRequest;
import com.skala.ch02.lab3.dto.HistoryMessage;
import com.skala.ch02.lab3.dto.Lab3ChatResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Day3 실습 — 상담 에이전트. 컨트롤러가 인증(여기서는 {@code userId} 파라미터로 단순화)에서
 * 꺼낸 사용자 ID 를 {@code toolContext}(도구용)와 {@code advisors} 파라미터(Advisor 용) 양쪽에
 * 동시에 심는다 — 두 채널이 서로 다르기 때문이다.
 */
@RestController
@RequestMapping("/lab3")
@Tag(name = "Day3 실습 · 상담 에이전트")
public class Lab3ChatController {

    private final ChatClient assistantChatClient;
    private final ChatMemory chatMemory;

    public Lab3ChatController(ChatClient assistantChatClient, ChatMemory chatMemory) {
        this.assistantChatClient = assistantChatClient;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/chat")
    @Operation(summary = "상담 에이전트와 대화", description = "규정 질문은 근거로, 주문 조회·환불 접수는 도구로 처리한다. "
            + "같은 sessionId 로 보내면 이전 턴을 기억한다.")
    public Lab3ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        return assistantChatClient.prompt()
                .user(request.message())
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, request.sessionId())
                        .param("userId", request.userId())
                        .param("traceId", traceId))
                .toolContext(Map.of("userId", request.userId(), "traceId", traceId))
                .call()
                .entity(Lab3ChatResponse.class);
    }

    @GetMapping("/chat/history")
    @Operation(summary = "대화 기록 확인", description = "세션 격리·Advisor 순서를 검증할 때 쓴다.")
    public List<HistoryMessage> history(
            @Parameter(description = "대화(세션) 식별자", example = "s1") @RequestParam String sessionId) {
        return chatMemory.get(sessionId).stream().map(HistoryMessage::from).toList();
    }
}
