package com.skala.ch02.lab3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.domain.RefundTicket.Status;
import com.skala.ch02.lab3.repository.RefundTicketRepository;

/**
 * Day3 실습 — 완료 기준 3번(승인 게이트). 도구는 PENDING 만 만들 수 있고, APPROVED 로의
 * 전이는 {@link RefundTicket#approve()} 를 통해서만 — 이미 처리된 티켓을 다시 승인하면
 * 예외로 막힌다(멱등하게 조용히 넘어가지 않는다 — 이중 승인은 실수의 신호다).
 */
@DataJpaTest
class Lab3RefundApprovalTest {

    @Autowired
    private RefundTicketRepository tickets;

    @Test
    @DisplayName("접수 직후 상태는 PENDING이다")
    void 접수하면_PENDING이다() {
        RefundTicket saved = tickets.save(new RefundTicket("12345", "user1", "단순 변심"));
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    @DisplayName("승인하면 APPROVED로 전이한다")
    void 승인하면_APPROVED다() {
        RefundTicket saved = tickets.save(new RefundTicket("12345", "user1", "단순 변심"));
        saved.approve();
        assertThat(saved.getStatus()).isEqualTo(Status.APPROVED);
    }

    @Test
    @DisplayName("이미 승인된 티켓을 다시 승인하면 예외가 난다")
    void 재승인은_예외다() {
        RefundTicket saved = tickets.save(new RefundTicket("12345", "user1", "단순 변심"));
        saved.approve();
        assertThatThrownBy(saved::approve).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("대기 목록에는 PENDING만 나온다")
    void 대기_목록은_PENDING만() {
        RefundTicket pending = tickets.save(new RefundTicket("12345", "user1", "단순 변심"));
        RefundTicket approved = tickets.save(new RefundTicket("99999", "user2", "오배송"));
        approved.approve();
        tickets.saveAndFlush(approved);

        assertThat(tickets.findByStatusOrderByRequestedAtAsc(Status.PENDING))
                .extracting(RefundTicket::getId)
                .containsExactly(pending.getId());
    }
}
