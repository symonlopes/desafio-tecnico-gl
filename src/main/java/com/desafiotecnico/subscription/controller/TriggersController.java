package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.request.SubscriptionRenewalTrigger;
import com.desafiotecnico.subscription.service.SubscriptionService;
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

    private final SubscriptionService subscriptionService;

    @PostMapping("/renewal")
    public ResponseEntity<Void> triggerRenovation(@RequestBody @Valid SubscriptionRenewalTrigger request) {
        subscriptionService.triggerRenovation(request.getMaxSubscriptions(), request.getDateToProcess());
        return ResponseEntity.ok().build();
    }
}
