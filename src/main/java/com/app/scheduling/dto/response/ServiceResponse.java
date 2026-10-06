package com.app.scheduling.dto.response;

import com.app.scheduling.model.Service;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal price,
        UserSummaryResponse professional,
        Boolean active
) {
    public static ServiceResponse fromEntity(Service service) {
        if (service == null) return null;
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                UserSummaryResponse.fromEntity(service.getProfessional()),
                service.getActive()
        );
    }
}
