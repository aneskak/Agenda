package com.app.scheduling.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        Long clientId,

        @NotNull(message = "O ID do serviço é obrigatório")
        Long serviceId,

        @NotNull(message = "A data e hora de início são obrigatórias")
        @Future(message = "A data e hora de início devem ser futuras")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
        LocalDateTime startDateTime,

        @Size(max = 1000, message = "As observações não podem exceder 1000 caracteres")
        String notes
) {}
