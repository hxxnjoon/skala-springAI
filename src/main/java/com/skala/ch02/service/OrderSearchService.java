package com.skala.ch02.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.dto.OrderDtos.OrderSearchCondition;
import com.skala.ch02.dto.OrderDtos.OrderStatistic;
import com.skala.ch02.mapper.OrderDtoMapper;
import com.skala.ch02.mapper.OrderMapper;

/**
 * 3장 — 조회 전용 Service(Mapper 를 쓰는 쪽).
 *
 * <p>{@link OrderService} 는 JPA Repository 를, 이 서비스는 MyBatis Mapper 를 쓴다.
 * <b>컨트롤러 입장에서는 차이가 없다</b> — 둘 다 {@code OrderResponse} 를 돌려준다.
 * 그것이 계층을 나눈 값이다. 데이터 접근 방식을 바꿔도 위쪽은 그대로다.
 *
 * <h3>왜 조회를 따로 두었나</h3>
 * 읽기와 쓰기는 요구가 다르다. 쓰기는 정합성(트랜잭션·검증)이, 읽기는 모양과 속도가
 * 중요하다. 규모가 커지면 이 둘을 아예 다른 모델로 나누기도 한다(CQRS).
 * 여기서는 <b>서비스 클래스를 나누는 정도</b>로 그 방향만 보여 준다.
 *
 * <p>{@code readOnly = true} 는 이 서비스 전체에 걸린다 — 쓰기 메서드가 아예 없다.
 */
@Service
@Transactional(readOnly = true)
public class OrderSearchService {

    private final OrderMapper orderMapper;         // 데이터 접근 — SQL 은 저쪽에
    private final OrderDtoMapper orderDtoMapper;   // 타입 변환 — 표현은 이쪽에

    public OrderSearchService(OrderMapper orderMapper, OrderDtoMapper orderDtoMapper) {
        this.orderMapper = orderMapper;
        this.orderDtoMapper = orderDtoMapper;
    }

    /**
     * 조건 검색 — 상태·키워드·정렬이 각각 있을 수도, 없을 수도 있다.
     *
     * <p>서비스가 하는 일은 <b>조건을 만들고 결과를 화면 모양으로 바꾸는 것</b>뿐이다.
     * SQL 은 Mapper 가, 변환은 DTO 매퍼가 한다 — 이 메서드에서 "어떻게" 는 보이지 않는다.
     */
    public List<OrderResponse> search(String userId, String status, String keyword, String sort) {
        OrderSearchCondition condition =
                new OrderSearchCondition(userId, status, keyword, sort);
        return orderDtoMapper.toResponses(orderMapper.search(condition));
    }

    /** 집계 — 이미 화면이 원하는 모양이라 변환 없이 그대로 내보낸다. */
    public List<OrderStatistic> statistics(String userId) {
        return orderMapper.statistics(userId);
    }

    /** 건수만 필요할 때 목록을 꺼내지 않는다 — 세는 일은 DB 가 한다. */
    public long count(String userId) {
        return orderMapper.countByOwner(userId);
    }
}
