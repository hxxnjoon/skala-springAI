package com.skala.ch02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.skala.ch02.dto.OrderDtos.OrderRow;
import com.skala.ch02.dto.OrderDtos.OrderSearchCondition;
import com.skala.ch02.dto.OrderDtos.OrderStatistic;

/**
 * 3장 — Mapper 계층(MyBatis).
 *
 * <p><b>Repository 와 같은 자리에 선다.</b> 둘 다 "데이터에 닿는 유일한 곳"이고,
 * 위 계층(Service)은 어느 쪽을 쓰는지 몰라도 된다. 다른 것은 <b>SQL 을 누가 쓰느냐</b>다.
 *
 * <table border="1">
 *   <caption>같은 프로젝트에서 나눠 쓰는 기준</caption>
 *   <tr><th></th><th>JPA Repository</th><th>MyBatis Mapper</th></tr>
 *   <tr><td>SQL</td><td>메서드 이름·JPQL 로 생성</td><td>내가 직접 쓴다</td></tr>
 *   <tr><td>잘 맞는 일</td><td>단건 CRUD·엔티티 상태 변경</td><td>동적 조건 검색·집계·통계·리포트</td></tr>
 *   <tr><td>반환</td><td>엔티티(영속 상태)</td><td>조회 전용 DTO(그냥 값)</td></tr>
 *   <tr><td>변경 감지</td><td>있다(dirty checking)</td><td>없다 — update 문을 직접 쓴다</td></tr>
 * </table>
 *
 * <p>이 예제는 <b>조회 전용</b>으로만 Mapper 를 쓴다. 쓰기는 JPA 가 하고,
 * 화면용 검색·집계는 Mapper 가 한다 — 실무에서 가장 흔한 분업이다.
 *
 * <h3>인터페이스만 있고 구현체가 없는 이유</h3>
 * {@code @Mapper} 가 붙은 인터페이스는 MyBatis 가 프록시 구현체를 만들어 빈으로 등록한다.
 * {@code JpaRepository} 와 똑같은 방식이다 — 그래서 서비스는 생성자 주입만 하면 된다.
 *
 * <p>SQL 을 두는 자리는 두 가지다.
 * <ul>
 *   <li><b>애노테이션</b>({@code @Select}) — 짧고 고정된 SQL. 아래 {@link #countByOwner} 참고.</li>
 *   <li><b>XML</b>({@code resources/mapper/OrderMapper.xml}) — 동적 SQL. 조건이 늘면 이쪽이 읽기 쉽다.</li>
 * </ul>
 */
@Mapper
public interface OrderMapper {

    /**
     * 동적 조건 검색 — SQL 은 {@code resources/mapper/OrderMapper.xml} 에 있다.
     *
     * <p>조건 세 개가 각각 있을 수도, 없을 수도 있다. 이런 쿼리를 JPA 로 쓰면
     * Specification·QueryDSL 같은 장치가 필요해진다. MyBatis 는 {@code <if>} 한 줄이다.
     *
     * <p><b>소유자 조건은 여기서도 빠지지 않는다.</b> {@code ownerId} 는 {@code <if>} 밖,
     * 즉 <b>항상 걸리는 자리</b>에 두었다 — 조건절에 섞어 두면 언젠가 빠진다.
     */
    List<OrderRow> search(OrderSearchCondition condition);

    /**
     * 집계 — 상태별 건수와 합계. 화면 하나를 위해 엔티티를 다 꺼낼 이유가 없다.
     *
     * <p>이런 쿼리가 Mapper 를 쓰는 대표적인 이유다. JPA 로 하면 엔티티를 전부
     * 메모리로 올린 뒤 자바에서 세게 되기 쉽다 — 건수가 늘면 그대로 장애가 된다.
     */
    List<OrderStatistic> statistics(@Param("ownerId") String ownerId);

    /** 짧고 고정된 SQL 은 애노테이션이 더 낫다 — XML 을 열어 볼 필요가 없다. */
    @Select("select count(*) from orders where owner_id = #{ownerId}")
    long countByOwner(@Param("ownerId") String ownerId);

    /**
     * {@code #{}} 와 {@code ${}} 는 완전히 다르다.
     *
     * <ul>
     *   <li>{@code #{item}} → {@code ?} 로 바인딩된다. <b>안전하다.</b></li>
     *   <li>{@code ${item}} → 문자열이 그대로 SQL 에 붙는다. <b>SQL 인젝션이다.</b></li>
     * </ul>
     *
     * <p>정렬 컬럼처럼 {@code ${}} 가 꼭 필요한 자리는 화이트리스트로 값을 제한한 뒤에 쓴다.
     * 이 예제의 정렬은 XML 안에서 허용된 컬럼만 매핑한다.
     */
    @Select("select id as orderId, item, status, ordered_at as orderedAt, eta "
          + "  from orders "
          + " where owner_id = #{ownerId} "
          + "   and item like '%' || #{keyword} || '%' "
          + " order by ordered_at desc")
    List<OrderRow> searchByKeyword(@Param("ownerId") String ownerId,
                                   @Param("keyword") String keyword);
}
