package com.skala.ch02.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.dto.OrderDtos.CreateOrderRequest;
import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.repository.OrderRepository;

/**
 * 2장 — Service 계층.
 *
 * <p>하는 일은 <b>조합</b>이다. 여러 Repository·외부 호출을 엮어 하나의 업무를 완성하고,
 * 트랜잭션 경계("전부 되거나 전부 안 되거나")를 긋는다.
 *
 * <p>판단 기준 하나 — 서비스 코드에서 SQL·HTTP 같은 <b>"어떻게 하는가"</b> 가 보이기
 * 시작하면 아래 계층으로 내려보낼 때다. 서비스는 <b>"무엇을 하는가"</b> 만 읽혀야 한다.
 *
 * <h3>@Transactional 을 클래스에 readOnly 로 거는 이유</h3>
 * 기본을 조회로 두고 쓰기 메서드에서만 재정의하면, 의도가 드러나고 실수로 쓰기가
 * 섞이는 것도 막는다. {@code readOnly = true} 는 JPA 에게 "변경 감지를 하지 말라"고
 * 알려 주는 것이기도 해서 성능에도 유리하다.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;   // 인터페이스에만 의존한다

    /** 실무에서는 채번 테이블이나 DB 시퀀스를 쓴다. 예제라 메모리 카운터로 대신한다. */
    private final AtomicLong orderNumberSequence = new AtomicLong(20_000);

    /** 생성자 주입 — 의존성이 시그니처에 드러나고, final 로 불변이 된다. */
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse find(String orderId, String userId) {
        Order order = orderRepository.findByIdAndOwnerId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order);            // 엔티티 → DTO
    }

    public List<OrderResponse> findRecent(String userId) {
        return orderRepository.findTop5ByOwnerIdOrderByOrderedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public List<OrderResponse> findInProgress(String userId) {
        return orderRepository.findByOwnerAndStatuses(userId,
                        List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.SHIPPING))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * 쓰기 — 여기서만 트랜잭션을 재정의한다.
     *
     * <p>이 메서드 안에서 예외가 나면 저장이 통째로 롤백된다. 그것이 트랜잭션 경계를
     * 서비스에 두는 이유다. Repository 마다 경계를 두면 "절반만 저장된" 상태가 생긴다.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request, String userId) {
        String orderId = String.valueOf(orderNumberSequence.incrementAndGet());

        Order order = new Order(orderId, userId, request.item(), OrderStatus.PAID,
                LocalDate.now(), LocalDate.now().plusDays(2),
                BigDecimal.valueOf(10_000L * request.quantity()));

        return OrderResponse.from(orderRepository.save(order));
    }
}
