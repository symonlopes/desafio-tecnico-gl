package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.request.PaymentCallbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final com.desafiotecnico.subscription.service.SubscriptionRenewalProducer renovationProducer;


    /*
    Apenas valida a mensagme de callback vinda do Gateway de Pagamento e a coloca em uma fila.
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> processCallback(@RequestBody @Valid PaymentCallbackRequest request) {
        log.info("Received payment callback for transaction: {}", request.getTransactionId());

        var event = com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse.builder()
                .transactionId(request.getTransactionId())
                .success(request.isSuccess())
                .message(request.getMessage())
                .build();

        renovationProducer.sendPaymentResponse(event);

        return ResponseEntity.ok().build();
    }
}
