package com.desafiotecnico.subscription.dto.request;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSignatureRequest {

    @NotNull(message = "O ID é obrigatório")
    private UUID id;

    @NotNull(message = "O ID do usuário é obrigatório")
    private UUID usuarioId;

    @NotNull(message = "O plano é obrigatório")
    private Plan plano;

    @NotNull(message = "A data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "A data de expiração é obrigatória")
    private LocalDate dataExpiracao;

    @NotNull(message = "O status é obrigatório")
    private SubscriptionStatus status;
}
