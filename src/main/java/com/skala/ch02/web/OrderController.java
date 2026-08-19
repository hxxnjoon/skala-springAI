package com.skala.ch02.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skala.ch02.dto.OrderDtos.CreateOrderRequest;
import com.skala.ch02.dto.OrderDtos.OrderResponse;
import com.skala.ch02.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 2장 — Controller 계층.
 *
 * <p>하는 일은 네 가지뿐이다 — 받고 · 검증하고 · 서비스에 넘기고 · 응답 형태로 돌려준다.
 * <b>업무 규칙은 넣지 않는다.</b> 여기에 {@code if} 가 쌓이기 시작하면
 * 업무 규칙이 새어 들어온 것이니 서비스로 옮긴다.
 *
 * <p>이 클래스는 Repository 를 모른다. 서비스만 안다 — 계층을 건너뛰지 않는다.
 *
 * <p>실습:
 * <pre>
 *   curl 'localhost:8080/ch02/orders/12345?userId=user1'   → 조회됨
 *   curl 'localhost:8080/ch02/orders/99999?userId=user1'   → 404 (남의 주문)
 *   curl 'localhost:8080/ch02/orders?userId=user1'
 *   curl -X POST localhost:8080/ch02/orders?userId=user1 \
 *        -H 'Content-Type: application/json' \
 *        -d '{"item":"키보드","quantity":2,"memo":"빠른 배송"}'
 *   curl -X POST localhost:8080/ch02/orders?userId=user1 \
 *        -H 'Content-Type: application/json' \
 *        -d '{"item":"","quantity":0}'                     → 400 (검증 실패)
 * </pre>
 */
@RestController
@RequestMapping("/ch02/orders")          // 공통 경로는 클래스에
@Tag(name = "3장 · 주문(Repository)",
     description = "JPA Repository 로 단건 조회·목록·생성을 수행한다. AI 를 쓰지 않아 키 없이 동작한다.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 단건 조회",
               description = """
                       소유자 조건을 **쿼리 안에서** 함께 건다(`findByIdAndOwnerId`).
                       남의 주문을 물으면 403 이 아니라 **404** 를 준다 —
                       "그런 주문이 있다"는 사실 자체가 정보이기 때문이다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "없는 주문이거나 남의 주문", content = @Content)
    })
    public OrderResponse find(
            @Parameter(description = "주문번호", example = "12345") @PathVariable String orderId,
            @Parameter(description = "조회 주체(실서비스라면 인증 정보에서 꺼낸다)", example = "user1")
            @RequestParam String userId) {
        // 실제 서비스라면 userId 는 Principal 에서 꺼낸다. 실습 편의를 위해 파라미터로 받았다.
        return orderService.find(orderId, userId);
    }

    @GetMapping
    @Operation(summary = "최근 주문 목록", description = "본인 주문 최근 5건을 주문일 역순으로 돌려준다.")
    public List<OrderResponse> findRecent(
            @Parameter(description = "조회 주체", example = "user1") @RequestParam String userId) {
        return orderService.findRecent(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "주문 생성",
               description = """
                       `@Valid` 가 요청 DTO 의 검증 규칙을 실행한다.
                       상품명이 비었거나 수량이 0이면 컨트롤러 코드가 실행되기 전에 **400** 이다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "검증 실패(항목별 메시지가 함께 온다)",
                         content = @Content)
    })
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request,
                                @Parameter(description = "주문 주체", example = "user1")
                                @RequestParam String userId) {
        return orderService.create(request, userId);   // 검증은 @Valid 가 대신한다
    }
}
