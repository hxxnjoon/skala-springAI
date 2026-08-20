package com.skala.ch02.lab3.dto;

import org.springframework.ai.chat.messages.Message;

import io.swagger.v3.oas.annotations.media.Schema;

public record HistoryMessage(
        @Schema(example = "USER") String role,
        @Schema(example = "단순 변심 반품은 며칠 이내인가요") String text) {

    public static HistoryMessage from(Message message) {
        return new HistoryMessage(message.getMessageType().name(), message.getText());
    }
}
