package com.skala.ch02.lab2.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Lab2AiConfig {

    @Bean // 근거 기반 답변 전용 클라이언트
    ChatClient ragChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0) // 근거가 같으면 답도 항상 같아야 한다
                        .build())
                .build();
    }
}
