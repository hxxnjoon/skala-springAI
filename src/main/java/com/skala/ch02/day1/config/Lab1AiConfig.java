package com.skala.ch02.day1.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Lab1AiConfig {

    @Bean // 용도별로 나눈다— 요약 전용 클라이언트
    ChatClient summaryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 이커머스 주문 상담 도우미다.
                        주어진 주문 정보만 사용해 한국어 한 문장으로 요약한다.
                        추측하지 않는다. 정보가 부족하면"정보가 부족합니다"라고 답한다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0) // 요약은 매번 같아야 한다
                        .maxTokens(120) // 비용 상한— 길게 쓸 이유가 없다
                        .build())
                .build();
    }
}
