package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;

import com.desafiotecnico.subscription.dto.response.SubscriptionResponse;
import com.desafiotecnico.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@RequestBody SubscriptionRequest request) {
        log.info("Request to create subscription for user: {}", request.getUserId());
        Subscription subscription = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.fromInternal(subscription));
    }

}
