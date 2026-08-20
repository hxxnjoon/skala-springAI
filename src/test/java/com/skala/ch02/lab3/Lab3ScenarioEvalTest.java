package com.skala.ch02.lab3;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.skala.ch02.domain.Order;
import com.skala.ch02.domain.Order.OrderStatus;
import com.skala.ch02.lab2.service.Lab2IngestService;
import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.dto.Lab3ChatResponse;
import com.skala.ch02.lab3.repository.RefundTicketRepository;
import com.skala.ch02.repository.OrderRepository;

/**
 * Day3 실습 Step5 — 한 대화 안에서 다섯 턴을 순서대로 진행한다. 실제 모델을 호출하므로
 * {@code @Tag("eval")} 로 기본 {@code ./gradlew test} 에서는 제외되고
 * {@code ./gradlew test -Peval} 로만 실행된다.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("eval")
class Lab3ScenarioEvalTest {

    private static final Logger log = LoggerFactory.getLogger(Lab3ScenarioEvalTest.class);

    @Autowired
    private ChatClient assistantChatClient;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private Lab2IngestService ingestService;
    @Autowired
    private RefundTicketRepository tickets;

    @BeforeAll
    void setUp() {
        ingestService.ingestSampleDocs();
        orders.save(new Order("12345", "user1", "무선 이어폰", OrderStatus.SHIPPING,
                LocalDate.of(2026, 7, 26), LocalDate.of(2026, 7, 30), new BigDecimal("52000")));
    }

    private Lab3ChatResponse turn(String userId, String sessionId, String message) {
        String traceId = "scenario-test";
        Lab3ChatResponse response = assistantChatClient.prompt()
                .user(message)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                        .param("userId", userId)
                        .param("traceId", traceId))
                .toolContext(Map.of("userId", userId, "traceId", traceId))
                .call()
                .entity(Lab3ChatResponse.class);
        log.info("[{}/{}] {} -> {}", sessionId, userId, message, response);
        return response;
    }

    @Test
    @DisplayName("5턴 시나리오 — RAG 출처, 도구 조회, 메모리, 승인 게이트, 세션 격리")
    void 오턴_시나리오를_순서대로_진행한다() {
        // 1턴 — RAG: 규정 답변 + 출처
        Lab3ChatResponse t1 = turn("user1", "s1", "단순 변심 반품은 며칠 이내인가요?");
        assertThat(t1.answer()).contains("7일");
        assertThat(t1.sources()).isNotEmpty();

        // 2턴 — 도구: 실시간 상태 조회
        Lab3ChatResponse t2 = turn("user1", "s1", "제 주문 12345는 지금 어디예요?");
        assertThat(t2.answer()).contains("12345");

        // 3턴 — 메모리: 1·2턴을 함께 참조(대명사 해석) — 답이 정확히 뭐라 나올지는 모델마다
        // 다를 수 있어 내용은 로그로 남기고, 최소한 응답이 왔는지만 단언한다.
        Lab3ChatResponse t3 = turn("user1", "s1", "그럼 그거 반품 돼요?");
        assertThat(t3.answer()).isNotBlank();

        // 4턴 — 승인 게이트: 접수(즉시 처리 아님)
        Lab3ChatResponse t4 = turn("user1", "s1", "환불로 접수해 주세요");
        assertThat(t4.answer()).containsAnyOf("접수", "승인");

        // 5턴 — 새 세션: 맥락 없음(되묻거나 모른다고 해야 한다)
        Lab3ChatResponse t5 = turn("user1", "s2", "그거 어떻게 됐어요?");
        assertThat(t5.answer()).isNotBlank();
        log.info("5턴(새 세션) 응답 — 맥락 있는 것처럼 답했다면 세션 격리 실패: {}", t5.answer());

        // 확인 — 4번 턴에서 만든 티켓 1건만 PENDING 으로 남아 있어야 한다(중복 접수 없음).
        assertThat(tickets.findByStatusOrderByRequestedAtAsc(RefundTicket.Status.PENDING))
                .hasSize(1)
                .first()
                .extracting(RefundTicket::getOrderId)
                .isEqualTo("12345");
    }
}
