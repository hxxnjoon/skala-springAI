package com.skala.ch02;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.repository.OrderRepository;

/**
 * 실습용 초기 데이터.
 *
 * <p>H2 는 인메모리라 앱을 띄울 때마다 비어 있다. 기동 직후 한 번 넣어 준다.
 * 실무에서는 마이그레이션 도구(Flyway·Liquibase)를 쓴다.
 *
 * <p>{@code ApplicationRunner} 는 앱 기동이 끝난 뒤 한 번 실행된다.
 * 테스트 슬라이스({@code @DataJpaTest})에서는 컴포넌트 스캔이 되지 않으므로
 * 이 빈이 만들어지지 않는다 — 테스트는 각자 필요한 데이터를 직접 넣는다.
 */
@Component
public class OrderDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderDataInitializer.class);

    private final OrderRepository orderRepository;

    public OrderDataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (orderRepository.count() > 0) {
            return;
        }
        orderRepository.saveAll(List.of(
                new Order("12345", "user1", "무선 이어폰", OrderStatus.SHIPPING,
                        LocalDate.of(2026, 7, 26), LocalDate.of(2026, 7, 30),
                        new BigDecimal("52000")),
                new Order("12346", "user1", "USB-C 케이블", OrderStatus.DELIVERED,
                        LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 18),
                        new BigDecimal("4000")),
                new Order("12347", "user1", "기계식 키보드", OrderStatus.PREPARING,
                        LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 1),
                        new BigDecimal("98000")),
                new Order("99999", "user2", "노트북 스탠드", OrderStatus.PAID,
                        LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2),
                        new BigDecimal("21000"))));

        log.info("ch02 초기 주문 데이터 {}건 적재", orderRepository.count());
    }
}
