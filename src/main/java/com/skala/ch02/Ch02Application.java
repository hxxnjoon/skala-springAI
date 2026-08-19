package com.skala.ch02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 교재 1장 — 계층 구조. <b>AI 를 쓰지 않는다.</b>
 *
 * <p>Spring AI 코드를 어디에 둘지 판단하려면 기준이 되는 구조가 먼저 있어야 한다.
 * JPA + H2 인메모리라 {@code @Entity} · {@code @Repository} · {@code @Transactional} 이
 * 설명이 아니라 실제로 동작한다. <b>OPENAI_API_KEY 가 아예 필요 없다.</b>
 *
 * <p>엔드포인트 — {@code /ch02/orders/**} · H2 콘솔 {@code /h2-console} *
 * <pre>
 *   ./gradlew bootRun          # http://localhost:8080
 *   http://localhost:8080/swagger-ui.html
 * </pre>
 */
@SpringBootApplication
public class Ch02Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch02Application.class, args);
    }
}
