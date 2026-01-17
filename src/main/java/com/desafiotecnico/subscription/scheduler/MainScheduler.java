package com.desafiotecnico.subscription.scheduler;

import com.desafiotecnico.subscription.service.TriggersService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainScheduler {

    private final TriggersService triggersService;

    @Value("${triggers.enqueue-transactions.limit}")
    private int enqueueTransactionsLimit;

    @Value("${triggers.generate-transactions.limit}")
    private int generateTransactionsLimit;

    @Scheduled(cron = "${triggers.generate-transactions.cron}")
    public void scheduleGenerateTransactions() {
        triggersService.generatePaymentTransactions(LocalDate.now());
    }

    @Scheduled(cron = "${triggers.enqueue-transactions.cron}")
    public void scheduleEnqueueTransactions() {
        triggersService.enqueuePaymentTransactions(enqueueTransactionsLimit, LocalDate.now());
    }
}
