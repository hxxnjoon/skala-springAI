package com.skala.ch02.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 2장 — 엔티티(도메인 객체).
 *
 * <p><b>이 객체는 API 로 나가지 않는다.</b> {@code ownerId}·{@code cost} 가 그대로
 * 노출되면 그것이 곧 사고다. 밖으로 나갈 때는 반드시 DTO 로 바꾼다.
 *
 * <h3>JPA 엔티티의 제약</h3>
 * <ul>
 *   <li>record 로 만들 수 없다 — JPA 가 기본 생성자와 가변 필드를 요구한다.</li>
 *   <li>기본 생성자는 {@code protected} 로 둔다 — 아무나 빈 객체를 만들지 못하게.</li>
 *   <li>{@code ORDER} 는 SQL 예약어라 테이블 이름을 {@code orders} 로 바꿨다.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
public class Order {

    /**
     * 주문번호는 사람이 읽는 업무 키다(예: "12345").
     * 실무에서는 채번 테이블이나 시퀀스로 만든다 — 여기서는 서비스가 만들어 넣는다.
     */
    @Id
    private String id;

    @Column(nullable = false)
    private String ownerId;          // 내부 전용 — 응답 DTO 에는 없다

    @Column(nullable = false)
    private String item;

    @Enumerated(EnumType.STRING)     // ORDINAL 은 순서가 바뀌면 데이터가 깨진다
    @Column(nullable = false)
    private OrderStatus status;

    private LocalDate orderedAt;
    private LocalDate eta;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;         // 원가 — 절대 노출하면 안 된다

    protected Order() {
        // JPA 전용
    }

    public Order(String id, String ownerId, String item, OrderStatus status,
                 LocalDate orderedAt, LocalDate eta, BigDecimal cost) {
        this.id = id;
        this.ownerId = ownerId;
        this.item = item;
        this.status = status;
        this.orderedAt = orderedAt;
        this.eta = eta;
        this.cost = cost;
    }

    public enum OrderStatus {
        PAID("결제완료"), PREPARING("상품준비중"), SHIPPING("배송중"), DELIVERED("배송완료");

        private final String label;

        OrderStatus(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 소유자 판단은 도메인 객체가 스스로 안다 — 서비스에 흩어 놓지 않는다. */
    public boolean isOwnedBy(String userId) {
        return ownerId.equals(userId);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getItem() {
        return item;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderedAt() {
        return orderedAt;
    }

    public LocalDate getEta() {
        return eta;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
