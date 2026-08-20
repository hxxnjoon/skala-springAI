package com.skala.ch02.lab3.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.ch02.lab3.domain.RefundTicket;
import com.skala.ch02.lab3.domain.RefundTicket.Status;

public interface RefundTicketRepository extends JpaRepository<RefundTicket, Long> {

    List<RefundTicket> findByStatusOrderByRequestedAtAsc(Status status);
}
