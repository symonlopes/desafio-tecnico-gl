package com.desafiotecnico.subscription.dto.response;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponse {

    private UUID id;

    @JsonProperty("usuarioId")
    private UUID userId;

    @JsonProperty("plano")
    private String plan;

    @JsonProperty("dataInicio")
    private LocalDate startDate;

    @JsonProperty("dataExpiracao")
    private LocalDate expirationDate;

    @JsonProperty("precoEmCentavos")
    private Integer priceInCents;

    @JsonProperty("statusRenovacao")
    private String renovationStatus;

    @JsonProperty("autoRenew")
    private Boolean autoRenew;

    public static SubscriptionResponse fromInternal(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .plan(subscription.getPlan())
                .autoRenew(subscription.getAutoRenew())
                .priceInCents(subscription.getPriceInCents())
                .startDate(subscription.getStartDate())
                .expirationDate(subscription.getExpirationDate())
                .build();
    }
}
