package com.skala.ch02.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;

/**
 * 2장 — Repository 계층.
 *
 * <p>인터페이스만 선언하면 Spring Data JPA 가 구현체를 만들어 준다.
 * {@code @Repository} 를 붙이지 않아도 빈으로 등록되고, DB 예외를
 * Spring Boot 의 {@code DataAccessException} 계열로 바꿔 주는 것도 동일하다.
 *
 * <p><b>여기가 데이터에 닿는 유일한 곳이다.</b> 서비스는 SQL 을 모르고,
 * 저장소를 바꿔도 위 계층은 그대로다.
 *
 * <h3>메서드 이름이 곧 쿼리다</h3>
 * {@code findByIdAndOwnerId} → {@code where id = ? and owner_id = ?}
 * 로 번역된다. 복잡해지면 {@code @Query} 로 직접 쓴다.
 */
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * 소유자 조건을 <b>쿼리 자체에</b> 넣는다.
     *
     * <p>{@code findById()} 로 꺼낸 뒤 자바 코드에서 소유자를 비교하는 방식은 위험하다.
     * 호출하는 곳 중 한 군데에서만 그 비교를 빠뜨려도 남의 데이터가 그대로 나간다.
     * 조건이 쿼리에 있으면 빠뜨릴 여지 자체가 없다.
     */
    Optional<Order> findByIdAndOwnerId(String id, String ownerId);

    /** 파생 쿼리 — 이름만으로 정렬·건수 제한까지 표현된다. */
    List<Order> findTop5ByOwnerIdOrderByOrderedAtDesc(String ownerId);

    /** 조건이 복잡해지면 JPQL 로 직접 쓴다. 여기서도 소유자 조건은 빠지지 않는다. */
    @Query("select o from Order o "
         + " where o.ownerId = :ownerId "
         + "   and o.status in :statuses "
         + " order by o.orderedAt desc")
    List<Order> findByOwnerAndStatuses(@Param("ownerId") String ownerId,
                                       @Param("statuses") List<OrderStatus> statuses);
}
