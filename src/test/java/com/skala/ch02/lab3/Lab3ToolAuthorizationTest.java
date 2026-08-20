package com.skala.ch02.lab3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.repository.RefundTicketRepository;
import com.skala.ch02.lab3.service.Lab3AuditLog;
import com.skala.ch02.lab3.tool.OrderTools;
import com.skala.ch02.repository.OrderRepository;

/**
 * Day3 실습 — 완료 기준 2번(권한 격리). {@code @DataJpaTest} 슬라이스라 모델을 부르지 않고도
 * "남의 주문은 조회되지 않는다"는 것 자체를 검증할 수 있다 — 이건 모델의 선의가 아니라
 * {@link OrderRepository#findByIdAndOwnerId} 가 쿼리 레벨에서 강제하기 때문이다.
 */
@DataJpaTest
@Import({OrderTools.class, Lab3AuditLog.class, SimpleMeterRegistry.class})
class Lab3ToolAuthorizationTest {

    @Autowired
    private OrderTools tools;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private RefundTicketRepository tickets;

    private ToolContext contextOf(String userId) {
        return new ToolContext(Map.of("userId", userId, "traceId", "test"));
    }

    @BeforeEach
    void setUp() {
        orders.saveAll(List.of(
                new Order("12345", "user1", "무선 이어폰", OrderStatus.SHIPPING,
                        LocalDate.of(2026, 7, 26), LocalDate.of(2026, 7, 30), new BigDecimal("52000")),
                new Order("99999", "user2", "노트북 스탠드", OrderStatus.PAID,
                        LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2), new BigDecimal("21000"))));
    }

    @Test
    @DisplayName("본인 주문은 도구로 조회된다")
    void 본인_주문_조회() {
        String result = tools.getOrderStatus("12345", contextOf("user1"));
        assertThat(result).contains("12345").contains("배송중");
    }

    @Test
    @DisplayName("남의 주문은 '찾을 수 없습니다' — 있음/없음을 구분해 알려주지 않는다")
    void 남의_주문은_차단된다() {
        String othersOrder = tools.getOrderStatus("99999", contextOf("user1"));
        String noOrder = tools.getOrderStatus("00000", contextOf("user1"));

        assertThat(othersOrder).isEqualTo(noOrder); // 같은 문구 — 존재 여부가 새어 나가지 않는다
        assertThat(othersOrder).contains("찾을 수 없습니다");
    }

    @Test
    @DisplayName("ToolContext 에 userId 가 없으면 예외 — 모델이 아니라 컨트롤러 버그다")
    void userId_없으면_예외() {
        assertThatThrownBy(() -> tools.getOrderStatus("12345", new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("환불 요청은 즉시 처리되지 않고 PENDING 티켓만 만든다")
    void 환불_요청은_접수만_한다() {
        String result = tools.requestRefund("12345", "단순 변심", contextOf("user1"));

        assertThat(result).contains("접수").contains("승인");
        assertThat(tickets.findAll()).hasSize(1);
        assertThat(tickets.findAll().get(0).getStatus()).isEqualTo(RefundTicket.Status.PENDING);
    }

    @Test
    @DisplayName("남의 주문은 환불 접수도 안 된다")
    void 남의_주문은_환불도_차단된다() {
        String result = tools.requestRefund("99999", "그냥요", contextOf("user1"));

        assertThat(result).contains("찾을 수 없습니다");
        assertThat(tickets.findAll()).isEmpty();
    }
}
