package com.skala.ch02.lab3.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Day3 실습 — 환불 승인 게이트.
 *
 * <p><b>도구(@Tool)는 이 엔티티를 {@code PENDING} 으로 만들 뿐, 상태를 바꾸지 못한다.</b>
 * {@code approve()} 를 호출하는 건 관리자 컨트롤러(사람이 누르는 버튼)뿐이고, 그 컨트롤러는
 * 모델에게 도구로 노출돼 있지 않다 — 되돌리기 어려운 행동은 모델이 원천적으로 닿지 못하는
 * 경로에 둔다.
 */
@Entity
@Table(name = "refund_tickets")
public class RefundTicket {

    public enum Status {
        PENDING, APPROVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;           // 요청한 사용자 — 승인 화면에서 누구 요청인지 보여준다

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant requestedAt;

    protected RefundTicket() {
        // JPA 전용
    }

    public RefundTicket(String orderId, String userId, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.status = Status.PENDING;
        this.requestedAt = Instant.now();
    }

    /** 상태 전이는 도메인 스스로 안다 — PENDING 이 아닌 걸 다시 승인하면 안 된다. */
    public void approve() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다: " + id);
        }
        this.status = Status.APPROVED;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
