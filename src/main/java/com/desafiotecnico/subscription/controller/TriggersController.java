package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.request.PaymentTransactionCreationRequest;
import com.desafiotecnico.subscription.dto.request.PaymentTransactionEnqueuRequest;
import com.desafiotecnico.subscription.service.TriggersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Esse controler permite chamar os métodos dos agendamentos. É útil para testes
 * e/ou utilização de agendados externos que permitem parametrização.
 */
@RestController
@RequestMapping("/triggers")
@RequiredArgsConstructor
public class TriggersController {

    private final TriggersService triggersService;

    @PostMapping("/generate-payment-transactions")
    public ResponseEntity<Void> generatePaymentTransactions(
            @RequestBody @Valid PaymentTransactionCreationRequest request) {
        triggersService.generatePaymentTransactions(request.getDateToProcess());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enqueue-payment-transactions")
    public ResponseEntity<Void> enqueuePaymentTransactions(
            @RequestBody @Valid PaymentTransactionEnqueuRequest request) {
        triggersService.enqueuePaymentTransactions(request.getLimit(), request.getDateToProcess());
        return ResponseEntity.ok().build();
    }
}
