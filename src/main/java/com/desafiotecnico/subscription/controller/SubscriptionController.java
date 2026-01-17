package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.dto.request.NewSignatureRequest;
import com.desafiotecnico.subscription.dto.request.SubscriptionCancelRequest;

import com.desafiotecnico.subscription.dto.response.SubscriptionResponse;
import com.desafiotecnico.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestBody @Valid NewSignatureRequest request) {
        log.info("Request to create subscription for user: {}", request.getUsuarioId());
        Subscription subscription = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.fromInternal(subscription));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable UUID id,
            @RequestBody @Valid SubscriptionCancelRequest request) {
        log.info("Request to cancel subscription: {}", id);
        subscriptionService.cancelSubscription(id, request.getReason());
        return ResponseEntity.noContent().build();
    }

}
