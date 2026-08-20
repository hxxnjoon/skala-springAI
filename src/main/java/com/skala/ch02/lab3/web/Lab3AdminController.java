package com.skala.ch02.lab3.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.dto.TicketView;
import com.skala.ch02.lab3.repository.RefundTicketRepository;
import com.skala.ch02.lab3.service.Lab3AuditLog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Day3 실습 — 승인 게이트의 사람 쪽. 이 컨트롤러의 메서드는 어디에도 {@code @Tool} 로
 * 등록돼 있지 않다 — 모델이 아무리 설득당해도 여기엔 원천적으로 닿을 수 없다.
 *
 * <p><b>운영 참고:</b> 이 프로젝트는 학습용이라 Spring Security 를 두지 않고 Day1~Day3
 * 내내 "userId 파라미터 = 인증 시뮬레이션"을 써 왔다. 실제 운영에서는 이 컨트롤러에
 * {@code @PreAuthorize("hasRole('ADMIN')")} 등 진짜 인가를 반드시 추가해야 한다 —
 * "모델이 못 부른다"와 "아무나 부를 수 있다"는 다른 문제다.
 */
@RestController
@RequestMapping("/lab3/admin/tickets")
@Tag(name = "Day3 실습 · 환불 승인(관리자)")
public class Lab3AdminController {

    private final RefundTicketRepository tickets;
    private final Lab3AuditLog auditLog;

    public Lab3AdminController(RefundTicketRepository tickets, Lab3AuditLog auditLog) {
        this.tickets = tickets;
        this.auditLog = auditLog;
    }

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 환불 목록")
    public List<TicketView> pending() {
        return tickets.findByStatusOrderByRequestedAtAsc(RefundTicket.Status.PENDING).stream()
                .map(TicketView::from)
                .toList();
    }

    @PostMapping("/{no}/approve")
    @Operation(summary = "환불 승인", description = "실행 버튼은 사람이 누른다 — 모델이 닿지 못하는 경로다.")
    public TicketView approve(@PathVariable Long no) {
        RefundTicket ticket = tickets.findById(no)
                .orElseThrow(() -> new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + no));
        ticket.approve();
        auditLog.toolCall("-", "adminApprove", String.valueOf(no), 0, true);
        return TicketView.from(ticket);
    }
}
