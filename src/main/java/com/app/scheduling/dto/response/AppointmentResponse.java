package com.app.scheduling.dto.response;

import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        UserSummaryResponse client,
        UserSummaryResponse professional,
        ServiceResponse service,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDateTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endDateTime,
        AppointmentStatus status,
        String externalCalendarEventId,
        String notes,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static AppointmentResponse fromEntity(Appointment appointment) {
        if (appointment == null) return null;
        return new AppointmentResponse(
                appointment.getId(),
                UserSummaryResponse.fromEntity(appointment.getClient()),
                UserSummaryResponse.fromEntity(appointment.getProfessional()),
                ServiceResponse.fromEntity(appointment.getService()),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getStatus(),
                appointment.getExternalCalendarEventId(),
                appointment.getNotes(),
                appointment.getCreatedAt()
        );
    }
}
