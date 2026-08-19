package com.skala.ch02.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.dto.OrderDtos.OrderRow;

/**
 * 3장 — DTO 매퍼(변환 담당).
 *
 * <p><b>이름은 같은 'Mapper' 지만 하는 일이 다르다.</b>
 * {@link OrderMapper} 는 DB 에 닿는 계층이고, 이 클래스는 <b>타입을 바꾸는 도구</b>다.
 * 같은 패키지에 나란히 두어 차이를 보이게 했다 — 실무에서 가장 헷갈리는 지점이다.
 *
 * <h3>변환을 어디서 하나</h3>
 * <ul>
 *   <li><b>Service 안</b> — 기본. 엔티티는 서비스 밖으로 나가지 않는다.</li>
 *   <li><b>Controller</b> — 표현 형식(날짜 포맷·화면 문구)만 다룰 때.</li>
 *   <li><b>Repository/Mapper</b> — 조회 전용 DTO 를 SQL 이 바로 채워 줄 때(가장 빠르다).</li>
 * </ul>
 *
 * <h3>세 가지 방식 — 무엇을 고르나</h3>
 * <table border="1">
 *   <caption>변환 코드를 두는 자리</caption>
 *   <tr><th>방식</th><th>언제</th><th>대가</th></tr>
 *   <tr><td>DTO 안 정적 팩터리<br>({@code OrderResponse.from})</td>
 *       <td>변환이 한두 개, 의존성이 없을 때</td><td>DTO 가 엔티티를 알게 된다</td></tr>
 *   <tr><td>매퍼 컴포넌트(이 클래스)</td>
 *       <td>변환에 다른 빈·규칙이 필요할 때</td><td>클래스가 하나 는다</td></tr>
 *   <tr><td>MapStruct</td>
 *       <td>필드가 많고 변환이 반복될 때</td><td>빌드 설정·생성 코드 읽는 법을 배워야 한다</td></tr>
 * </table>
 *
 * <p>MapStruct 로 쓰면 이 클래스 전체가 아래 다섯 줄이 된다.
 * 구현체는 컴파일 시점에 생성되므로 리플렉션 비용이 없다.
 * <pre>{@code
 * @org.mapstruct.Mapper(componentModel = "spring")
 * public interface OrderDtoMapper {
 *     @Mapping(target = "orderId", source = "id")
 *     @Mapping(target = "status",  expression = "java(order.getStatus().label())")
 *     OrderResponse toResponse(Order order);
 *     List<OrderResponse> toResponses(List<Order> orders);
 * }
 * }</pre>
 *
 * <p>이 예제는 의존성을 늘리지 않으려고 손으로 썼다. 어느 쪽이든
 * <b>변환 코드가 한 곳에 모여 있다</b>는 점이 핵심이다 — 흩어지면 필드를 빠뜨린다.
 */
@Component
public class OrderDtoMapper {

    /** 엔티티 → 응답 DTO. 노출할 필드만 옮긴다(ownerId·cost 는 옮기지 않는다). */
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getItem(),
                order.getStatus().label(), order.getOrderedAt(), order.getEta());
    }

    /**
     * Mapper 가 읽어 온 row → 응답 DTO.
     *
     * <p>입구가 둘(엔티티·row)이어도 <b>출구는 하나</b>다. 화면이 보는 모양이
     * 데이터를 어디서 읽었는지에 따라 달라지면 그때부터 프런트가 고생한다.
     */
    public OrderResponse toResponse(OrderRow row) {
        return new OrderResponse(row.orderId(), row.item(),
                label(row.status()), row.orderedAt(), row.eta());
    }

    public List<OrderResponse> toResponses(List<OrderRow> rows) {
        return rows.stream().map(this::toResponse).toList();
    }

    /**
     * 코드값 → 화면 문구.
     *
     * <p>DB 가 모르는 값이 들어와도 화면이 죽지 않게 <b>원문을 그대로 돌려준다</b>.
     * 변환기에서 예외를 던지면 데이터 한 건 때문에 목록 전체가 실패한다.
     */
    private String label(String statusCode) {
        try {
            return OrderStatus.valueOf(statusCode).label();
        } catch (IllegalArgumentException e) {
            return statusCode;
        }
    }
}
