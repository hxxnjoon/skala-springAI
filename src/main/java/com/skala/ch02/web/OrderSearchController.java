package com.skala.ch02.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.dto.OrderDtos.OrderStatistic;
import com.skala.ch02.service.OrderSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 3장 — Mapper 계층을 쓰는 Controller, 그리고 <b>Swagger 문서화의 본보기</b>.
 *
 * <p>같은 컨트롤러라도 애노테이션이 붙으면 문서가 달라진다.
 * 아래 세 엔드포인트를 <a href="http://localhost:8080/swagger-ui.html">Swagger UI</a> 에서
 * 열어 보면 설명·예시·응답 코드가 모두 채워져 있다 — {@link OrderController} 와 비교해 보라.
 *
 * <h3>문서화 애노테이션 지도</h3>
 * <ul>
 *   <li>{@code @Tag} — 컨트롤러 묶음 이름. Swagger UI 의 아코디언 한 칸이 된다.</li>
 *   <li>{@code @Operation} — 엔드포인트 한 줄 설명(summary)과 상세(description).</li>
 *   <li>{@code @Parameter} — 쿼리·경로 파라미터 설명과 <b>example</b>(Try it out 기본값).</li>
 *   <li>{@code @ApiResponse} — 상태 코드별 의미. 실패 응답을 적어 두는 것이 진짜 문서다.</li>
 *   <li>{@code @Schema} — DTO 필드 설명. 응답 모델 탭에 그대로 보인다.</li>
 * </ul>
 */
@RestController
@RequestMapping("/ch02/orders")
@Tag(name = "3장 · 주문 검색(Mapper)",
     description = "MyBatis Mapper 로 동적 조건 검색·집계를 수행한다. 같은 DB 를 JPA 와 나눠 쓴다.")
public class OrderSearchController {

    private final OrderSearchService orderSearchService;

    public OrderSearchController(OrderSearchService orderSearchService) {
        this.orderSearchService = orderSearchService;
    }

    @GetMapping("/search")
    @Operation(summary = "주문 조건 검색",
               description = """
                       상태 · 키워드 · 정렬을 조합해 검색한다. 조건은 전부 선택이며,
                       주어진 것만 `where` 절에 붙는다(MyBatis `<if>`).

                       소유자 조건(`userId`)은 조건과 무관하게 **항상** 걸린다 —
                       빠질 수 있는 자리에 두면 언젠가 빠지기 때문이다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공(0건이어도 200)",
                         content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 파라미터(userId) 누락", content = @Content)
    })
    public List<OrderResponse> search(
            @Parameter(description = "조회 주체. 실서비스라면 인증 정보에서 꺼낸다.",
                       example = "user1", required = true)
            @RequestParam String userId,

            @Parameter(description = "주문 상태 코드. 비우면 전체.",
                       schema = @Schema(allowableValues = {"PAID", "PREPARING", "SHIPPING", "DELIVERED"}),
                       example = "SHIPPING")
            @RequestParam(required = false) String status,

            @Parameter(description = "상품명 부분 검색어. 비우면 전체.", example = "이어폰")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "정렬 기준. 허용된 값만 SQL 에 들어간다(화이트리스트).",
                       schema = @Schema(allowableValues = {"orderedAt", "eta", "item"},
                                        defaultValue = "orderedAt"))
            @RequestParam(required = false) String sort) {

        return orderSearchService.search(userId, status, keyword, sort);
    }

    @GetMapping("/statistics")
    @Operation(summary = "상태별 주문 집계",
               description = """
                       상태별 건수와 금액 합계를 **SQL 한 번**으로 계산한다.
                       엔티티를 전부 꺼내 자바에서 세지 않는다 — 건수가 늘면 그대로 장애가 된다.
                       """)
    @ApiResponse(responseCode = "200", description = "집계 성공")
    public List<OrderStatistic> statistics(
            @Parameter(description = "조회 주체", example = "user1", required = true)
            @RequestParam String userId) {
        return orderSearchService.statistics(userId);
    }

    @GetMapping("/count")
    @Operation(summary = "주문 건수",
               description = "목록을 꺼내지 않고 건수만 센다. 애노테이션 SQL(`@Select`) 예시다.")
    public long count(
            @Parameter(description = "조회 주체", example = "user1", required = true)
            @RequestParam String userId) {
        return orderSearchService.count(userId);
    }
}
