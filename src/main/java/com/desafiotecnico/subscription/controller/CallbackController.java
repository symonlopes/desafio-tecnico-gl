package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.request.PaymentCallbackRequest;
import com.desafiotecnico.subscription.service.SubscriptionRenewalProducer;
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
public class CallbackController {

    private final SubscriptionRenewalProducer renovationProducer;

    /*
    Apenas valida a mensagem de callback do Gateway de Pagamento e a coloca numa fila.
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> processCallback(@RequestBody @Valid PaymentCallbackRequest request) {
        log.info("Recebendo mensagem de callback de paramento para transação: {}.",
                request.getTransactionId());

        var event = PaymentGatewayResponse.builder()
                .transactionId(request.getTransactionId())
                .success(request.isSuccess())
                .message(request.getMessage())
                .build();

        renovationProducer.sendPaymentResponse(event);

        return ResponseEntity.ok().build();
    }
}
