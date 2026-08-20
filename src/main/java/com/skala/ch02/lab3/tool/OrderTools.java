package com.skala.ch02.lab3.tool;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.ch02.domain.Order;
import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.repository.RefundTicketRepository;
import com.skala.ch02.lab3.service.Lab3AuditLog;
import com.skala.ch02.repository.OrderRepository;

/**
 * Day3 실습 — 상담 에이전트가 쓰는 도구.
 *
 * <p>모델은 코드를 보지 않는다 — {@link Tool#description()}만 본다. 사용자 ID 는
 * {@link ToolParam} 이 아니라 {@link ToolContext} 로 받는다 — 모델이 프롬프트로 흉내 낼 수
 * 있는 파라미터가 아니라, 컨트롤러가 인증에서 꺼내 심어 준 값만 신뢰한다.
 */
@Component
public class OrderTools {

    private final OrderRepository orders;
    private final RefundTicketRepository tickets;
    private final Lab3AuditLog auditLog;
    private final MeterRegistry registry;

    public OrderTools(OrderRepository orders, RefundTicketRepository tickets,
                       Lab3AuditLog auditLog, MeterRegistry registry) {
        this.orders = orders;
        this.tickets = tickets;
        this.auditLog = auditLog;
        this.registry = registry;
    }

    @Tool(description = """
            주문 상태를 조회한다. 사용자가 주문번호를 말하거나
            '내 주문', '배송 언제' 처럼 물으면 이 도구를 쓴다.
            """)
    public String getOrderStatus(
            @ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
            ToolContext context) {
        String userId = currentUser(context);
        long started = System.nanoTime();
        String result = orders.findByIdAndOwnerId(orderId, userId) // 권한은 쿼리 안에 — 있음/없음/남의 것을 구분하지 않는다
                .map(this::describe)
                .orElse("해당 주문을 찾을 수 없습니다.");
        record(context, "getOrderStatus", orderId, started, true);
        return result;
    }

    @Tool(description = """
            환불을 요청한다. 이 도구는 요청을 접수만 하며, 실제 환불은 처리되지 않는다 —
            담당자 승인 후 처리된다. 사용자가 '환불해줘', '취소하고 싶어요' 라고 하면 쓴다.
            """)
    public String requestRefund(
            @ToolParam(description = "환불할 주문번호") String orderId,
            @ToolParam(description = "환불 사유") String reason,
            ToolContext context) {
        String userId = currentUser(context);
        long started = System.nanoTime();

        Order order = orders.findByIdAndOwnerId(orderId, userId).orElse(null);
        if (order == null) {
            record(context, "requestRefund", orderId, started, false);
            return "해당 주문을 찾을 수 없습니다.";
        }

        RefundTicket ticket = tickets.save(new RefundTicket(orderId, userId, reason)); // 접수만 — 처리는 사람이 한다
        record(context, "requestRefund", orderId, started, true);
        return "환불 요청을 %d번으로 접수했습니다. 담당자 승인 후 처리됩니다.".formatted(ticket.getId());
    }

    private String describe(Order o) {
        return "%s: %s, 도착예정 %s".formatted(o.getId(), o.getStatus().label(), o.getEta());
    }

    private String currentUser(ToolContext context) {
        Object userId = context == null ? null : context.getContext().get("userId");
        if (userId == null) {
            throw new IllegalStateException("ToolContext 에 userId 가 없습니다 — 컨트롤러에서 넣어야 합니다.");
        }
        return userId.toString();
    }

    private void record(ToolContext context, String tool, String arg, long startedNanos, boolean ok) {
        long elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000;
        String traceId = context.getContext().getOrDefault("traceId", "-").toString();
        auditLog.toolCall(traceId, tool, arg, elapsedMs, ok);
        registry.counter("ai.tool.calls", "tool", tool, "result", ok ? "ok" : "fail").increment();
    }
}
