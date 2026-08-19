package com.skala.ch02.service;

/**
 * 업무 예외 — 서비스에서 던지고, 응답으로 바꾸는 일은
 * {@code @RestControllerAdvice} 한 곳에서 한다.
 *
 * <p>"없는 주문"과 "남의 주문"을 <b>구분해서 알려 주지 않는다</b>.
 * 구분하는 순간 "이 번호는 존재한다"는 정보가 새어 나간다.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("주문을 찾을 수 없습니다: " + orderId);
    }
}
