package com.skala.ch02;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.skala.ch02.dto.OrderDtos.OrderRow;
import com.skala.ch02.dto.OrderDtos.OrderSearchCondition;
import com.skala.ch02.dto.OrderDtos.OrderStatistic;
import com.skala.ch02.mapper.OrderMapper;

/**
 * 3장 — Mapper 계층 테스트.
 *
 * <p>{@code @MybatisTest} 는 <b>Mapper 와 DataSource 만</b> 띄우는 슬라이스다.
 * 웹·Spring AI·JPA 가 전혀 로딩되지 않아 <b>API 키 없이</b> 돌고 빠르다
 * ({@code @DataJpaTest} 와 같은 발상이다).
 *
 * <p>여기서 검증할 것은 <b>SQL 이 의도대로 도는가</b>다.
 * 동적 조건이 붙었다 빠지는지, 소유자 조건이 빠지지 않는지 — 이 둘이 핵심이다.
 * 서비스 로직은 이 테스트가 볼 일이 아니다.
 */
@MybatisTest
@Sql("/sql/orders.sql")
class OrderMapperTest {

    @Autowired
    private OrderMapper mapper;

    @Test
    @DisplayName("조건이 하나도 없으면 본인 주문 전체가 나온다 — <where> 가 where 절을 지운다")
    void 조건이_없으면_전체() {
        List<OrderRow> rows = mapper.search(new OrderSearchCondition("user1", null, null, null));

        assertThat(rows).hasSize(4)
                .extracting(OrderRow::orderId)
                .containsExactly("12347", "12345", "12348", "12346");   // ordered_at 내림차순
    }

    @Test
    @DisplayName("상태 조건이 붙으면 그 상태만 나온다")
    void 상태로_거른다() {
        List<OrderRow> rows = mapper.search(new OrderSearchCondition("user1", "SHIPPING", null, null));

        assertThat(rows).extracting(OrderRow::orderId).containsExactly("12345", "12348");
        assertThat(rows).extracting(OrderRow::status).containsOnly("SHIPPING");
    }

    @Test
    @DisplayName("키워드는 부분 일치로 걸린다")
    void 키워드로_거른다() {
        List<OrderRow> rows = mapper.search(new OrderSearchCondition("user1", null, "키보드", null));

        assertThat(rows).extracting(OrderRow::item)
                .containsExactlyInAnyOrder("기계식 키보드", "무선 키보드");
    }

    @Test
    @DisplayName("정렬 조건은 허용된 값만 반영된다 — 나머지는 기본 정렬")
    void 정렬은_화이트리스트() {
        List<OrderRow> byEta = mapper.search(new OrderSearchCondition("user1", null, null, "eta"));
        assertThat(byEta).extracting(OrderRow::orderId)
                .containsExactly("12346", "12348", "12345", "12347");   // eta 오름차순

        // 허용되지 않은 값(예: SQL 조각)은 조건 객체가 기본값으로 되돌린다
        List<OrderRow> injected = mapper.search(
                new OrderSearchCondition("user1", null, null, "id; drop table orders"));
        assertThat(injected).hasSize(4);                                 // 기본 정렬로 정상 조회
    }

    @Test
    @DisplayName("소유자 조건은 어떤 경우에도 빠지지 않는다")
    void 남의_주문은_절대_안_나온다() {
        assertThat(mapper.search(new OrderSearchCondition("user1", null, null, null)))
                .extracting(OrderRow::orderId).doesNotContain("99999");

        assertThat(mapper.search(new OrderSearchCondition("user2", null, null, null)))
                .extracting(OrderRow::orderId).containsExactly("99999");
    }

    @Test
    @DisplayName("집계는 SQL 한 번으로 — 상태별 건수와 합계")
    void 집계가_계산된다() {
        List<OrderStatistic> stats = mapper.statistics("user1");

        assertThat(stats).extracting(OrderStatistic::status)
                .containsExactlyInAnyOrder("SHIPPING", "DELIVERED", "PREPARING");
        assertThat(stats).filteredOn(s -> s.status().equals("SHIPPING"))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.statusLabel()).isEqualTo("배송중");        // case 문이 문구까지 만든다
                    assertThat(s.count()).isEqualTo(2);
                    assertThat(s.totalCost()).isEqualByComparingTo("119000");
                });
    }

    @Test
    @DisplayName("건수만 필요할 때는 목록을 꺼내지 않는다 — 애노테이션 SQL")
    void 건수만_센다() {
        assertThat(mapper.countByOwner("user1")).isEqualTo(4);
        assertThat(mapper.countByOwner("user2")).isEqualTo(1);
        assertThat(mapper.countByOwner("nobody")).isZero();
    }
}
