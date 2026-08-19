package com.skala.ch02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.dto.OrderDtos.CreateOrderRequest;
import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.repository.OrderRepository;
import com.skala.ch02.service.OrderNotFoundException;
import com.skala.ch02.service.OrderService;

/**
 * 2장 — Service + Repository 계층 테스트.
 *
 * <p>{@code @DataJpaTest} 는 <b>JPA 관련 빈만</b> 띄우는 슬라이스 테스트다.
 * Spring AI 자동 구성이나 웹 계층은 전혀 로딩되지 않으므로 <b>API 키 없이도</b> 돌고,
 * 전체 컨텍스트를 띄우는 것보다 훨씬 빠르다.
 * 여기에 {@code @Import} 로 검증할 서비스만 얹는다.
 *
 * <p>각 테스트는 트랜잭션 안에서 실행되고 끝나면 롤백된다 — 서로 간섭하지 않는다.
 */
@DataJpaTest
@Import(OrderService.class)
class OrderServiceLayerTest {

    @Autowired
    private OrderService service;

    @Autowired
    private OrderRepository repository;

    @BeforeEach
    void setUp() {
        // 초기 데이터는 테스트가 직접 넣는다(ApplicationRunner 는 슬라이스에서 돌지 않는다)
        repository.saveAll(List.of(
                new Order("12345", "user1", "무선 이어폰", OrderStatus.SHIPPING,
                        LocalDate.of(2026, 7, 26), LocalDate.of(2026, 7, 30),
                        new BigDecimal("52000")),
                new Order("12346", "user1", "USB-C 케이블", OrderStatus.DELIVERED,
                        LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 18),
                        new BigDecimal("4000")),
                new Order("99999", "user2", "노트북 스탠드", OrderStatus.PAID,
                        LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2),
                        new BigDecimal("21000"))));
    }

    @Test
    @DisplayName("본인 주문은 조회되고, 응답 DTO 에는 내부 필드가 없다")
    void 본인_주문은_조회된다() {
        OrderResponse response = service.find("12345", "user1");

        assertThat(response.orderId()).isEqualTo("12345");
        assertThat(response.status()).isEqualTo("배송중");
        // OrderResponse 에는 ownerId·cost 가 아예 없다 — 컴파일 시점에 보장된다
    }

    @Test
    @DisplayName("남의 주문은 조회되지 않는다 — 조건이 쿼리에 들어 있다")
    void 타인_주문은_차단된다() {
        // 99999 는 user2 의 주문이다. where 절에 owner_id 가 있으니 애초에 안 걸린다.
        assertThatThrownBy(() -> service.find("99999", "user1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("없는 주문도 같은 예외로 처리된다 — 존재 여부를 구분해 알려 주지 않는다")
    void 없는_주문도_같은_예외다() {
        assertThatThrownBy(() -> service.find("00000", "user1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("최근 주문 목록에는 본인 것만, 최신순으로 나온다")
    void 최근_주문은_본인것만() {
        assertThat(service.findRecent("user1"))
                .extracting(OrderResponse::orderId)
                .containsExactly("12345", "12346");     // orderedAt 내림차순
    }

    @Test
    @DisplayName("진행 중 주문만 골라내는 JPQL 이 동작한다")
    void 진행중_주문만_조회된다() {
        assertThat(service.findInProgress("user1"))
                .extracting(OrderResponse::orderId)
                .containsExactly("12345");              // DELIVERED 인 12346 은 제외
    }

    @Test
    @DisplayName("주문을 생성하면 실제로 저장된다 — @Transactional 이 걸려 있다")
    void 주문을_생성한다() {
        OrderResponse created = service.create(
                new CreateOrderRequest("기계식 키보드", 2, "빠른 배송 부탁"), "user1");

        assertThat(created.orderId()).isNotBlank();
        assertThat(created.item()).isEqualTo("기계식 키보드");

        // DB 에서 다시 읽어도 있다
        assertThat(repository.findByIdAndOwnerId(created.orderId(), "user1")).isPresent();
        assertThat(repository.findByIdAndOwnerId(created.orderId(), "user2")).isEmpty();
    }
}
