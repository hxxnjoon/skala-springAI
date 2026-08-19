package com.skala.ch02.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.skala.ch02.domain.Order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 2장 — 계층 사이의 경계에 놓는 DTO.
 *
 * <p>DTO 는 귀찮은 중복이 아니라 <b>방화벽</b>이다.
 * 엔티티를 그대로 내보내면 DB 구조가 곧 API 스펙이 되고,
 * 원가·내부 ID 같은 필드가 실수로 새어 나간다.
 *
 * <p>엔티티와 달리 DTO 는 <b>record 로 만들 수 있다</b> — 불변이고, JPA 제약이 없다.
 */
public final class OrderDtos {

    private OrderDtos() {}

    /**
     * 요청 DTO — 검증 규칙을 여기에 선언하면 컨트롤러에 if 를 쌓지 않아도 된다.
     *
     * <p>{@code @Schema} 로 붙인 설명과 예시는 Swagger UI 의 [Try it out] 기본값이 된다.
     * 검증 규칙({@code @NotBlank}·{@code @Min})도 문서에 <b>자동으로</b> 반영된다 —
     * 문서와 코드가 어긋날 수 없는 이유다.
     */
    @Schema(name = "CreateOrderRequest", description = "주문 생성 요청")
    public record CreateOrderRequest(
            @Schema(description = "상품명", example = "기계식 키보드", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "상품명은 필수입니다")
            String item,

            @Schema(description = "수량(1~99)", example = "2", minimum = "1", maximum = "99")
            @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
            @Max(value = 99, message = "수량은 99개를 넘을 수 없습니다")
            int quantity,

            @Schema(description = "배송 메모(선택, 200자 이내)", example = "부재 시 경비실")
            @Size(max = 200, message = "메모는 200자를 넘을 수 없습니다")
            String memo) { }

    /** 응답 DTO — 내보낼 필드만 고른다. ownerId·cost 는 여기에 아예 없다. */
    @Schema(name = "OrderResponse", description = "주문 응답 — 내부 필드(ownerId·cost)는 담기지 않는다")
    public record OrderResponse(
            @Schema(description = "주문번호", example = "12345") String orderId,
            @Schema(description = "상품명", example = "무선 이어폰") String item,
            @Schema(description = "상태(화면 문구)", example = "배송중") String status,
            @Schema(description = "주문일", example = "2026-07-26") LocalDate orderedAt,
            @Schema(description = "도착 예정일", example = "2026-07-30") LocalDate eta) {

        /** 변환은 한 곳에서 — 흩어지면 어딘가에서 반드시 필드를 빠뜨린다. */
        public static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getItem(),
                    order.getStatus().label(), order.getOrderedAt(), order.getEta());
        }
    }

    // ── 3장 Mapper 계층(MyBatis)용 ─────────────────────────────────
    // 아래 세 개는 "조회 전용" 타입이다. 엔티티가 아니라서 영속성 컨텍스트와 무관하고,
    // 화면이 필요한 모양 그대로 SQL 이 채워 준다.

    /**
     * 검색 조건 — Mapper 에 그대로 넘기는 파라미터 객체.
     *
     * <p>파라미터가 셋을 넘어가면 {@code @Param} 을 나열하는 대신 이렇게 묶는다.
     * XML 안에서는 {@code #{status}} 처럼 필드 이름으로 바로 꺼내 쓴다.
     *
     * <p>{@code sort} 는 컬럼 이름이 SQL 에 그대로 들어가는 자리라
     * <b>화이트리스트로 제한</b>한다 — 사용자가 준 문자열을 그대로 쓰면 인젝션이다.
     */
    public record OrderSearchCondition(String ownerId,
                                       String status,
                                       String keyword,
                                       String sort) {

        private static final List<String> ALLOWED_SORT = List.of("orderedAt", "eta", "item");

        /** 허용된 값이 아니면 기본값으로 되돌린다 — 거절보다 안전한 기본값이 낫다. */
        public OrderSearchCondition {
            sort = (sort != null && ALLOWED_SORT.contains(sort)) ? sort : "orderedAt";
        }
    }

    /**
     * Mapper 가 돌려주는 <b>한 줄(row)</b>.
     *
     * <p>{@code status} 는 DB 에 저장된 코드값 그대로다(PAID·SHIPPING…).
     * 화면 문구("배송중")로 바꾸는 일은 DTO 매퍼가 한다 — SQL 은 표현을 모른다.
     */
    public record OrderRow(String orderId,
                           String item,
                           String status,
                           LocalDate orderedAt,
                           LocalDate eta) { }

    /** 집계 결과 — 상태별 건수와 금액. 이미 화면이 원하는 모양이라 변환이 필요 없다. */
    @Schema(name = "OrderStatistic", description = "상태별 집계 — SQL 한 번으로 계산한다")
    public record OrderStatistic(
            @Schema(description = "상태 코드", example = "SHIPPING") String status,
            @Schema(description = "상태 문구", example = "배송중") String statusLabel,
            @Schema(description = "건수", example = "3") long count,
            @Schema(description = "금액 합계", example = "154000") BigDecimal totalCost) { }
}
