package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.request.PaymentTransactionEnqueuRequest;
import com.desafiotecnico.subscription.service.TriggersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triggers")
@RequiredArgsConstructor
public class TriggersController {

    private final TriggersService triggersService;

    @PostMapping("/generate-payment-transactions")
    public ResponseEntity<Void> generatePaymentTransactions() {
        triggersService.generatePaymentTransactions();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enqueue-payment-transactions")
    public ResponseEntity<Void> enqueuePaymentTransactions(
            @RequestBody @Valid PaymentTransactionEnqueuRequest request) {
        triggersService.enqueuePaymentTransactions(request.getLimit());
        return ResponseEntity.ok().build();
    }
}
