package com.skala.ch02.lab3.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.skala.ch02.lab3.domain.RefundTicket;

public record TicketView(
        @Schema(example = "1") Long no,
        @Schema(example = "12345") String orderId,
        @Schema(example = "PENDING") String status,
        @Schema(example = "단순 변심") String reason,
        Instant requestedAt) {

    public static TicketView from(RefundTicket t) {
        return new TicketView(t.getId(), t.getOrderId(), t.getStatus().name(), t.getReason(), t.getRequestedAt());
    }
}
