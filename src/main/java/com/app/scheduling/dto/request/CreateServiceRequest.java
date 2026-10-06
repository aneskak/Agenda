package com.app.scheduling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "O nome do serviço é obrigatório")
        @Size(max = 255, message = "O nome não pode ultrapassar 255 caracteres")
        String name,

        @Size(max = 1000, message = "A descrição não pode ultrapassar 1000 caracteres")
        String description,

        @NotNull(message = "A duração em minutos é obrigatória")
        @Min(value = 10, message = "A duração mínima do serviço é de 10 minutos")
        Integer durationMinutes,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "O preço não pode ser negativo")
        BigDecimal price,

        @NotNull(message = "O ID do profissional responsável é obrigatório")
        Long professionalId
) {}
