package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.repository.ServiceRepository;
import com.app.scheduling.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AppointmentConflictIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    @DisplayName("Deve impedir agendamento com conflito de horário para o mesmo profissional")
    void shouldThrowConflictExceptionWhenSlotIsAlreadyBooked() {
        // Arrange
        User client = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("lucas.pereira@email.com"))
                .findFirst().orElseThrow();

        Service service = serviceRepository.findAll().stream()
                .filter(s -> s.getName().equals("Manicure Tradicional"))
                .findFirst().orElseThrow();

        // O DataInitializerRunner já criou um agendamento amanhã às 14:00 (14:00 às 14:30)
        LocalDateTime conflictingStart = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).withHour(14);

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                client.getId(),
                service.getId(),
                conflictingStart,
                "Tentativa de agendamento no mesmo horário"
        );

        // Act & Assert (Regra 1: HTTP 409 / ScheduleConflictException)
        assertThrows(ScheduleConflictException.class, () -> appointmentService.create(request));
    }

    @Test
    @DisplayName("Deve permitir agendamento em horário livre")
    void shouldCreateAppointmentWhenSlotIsAvailable() {
        User client = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("lucas.pereira@email.com"))
                .findFirst().orElseThrow();

        Service service = serviceRepository.findAll().stream()
                .filter(s -> s.getName().equals("Manicure Tradicional"))
                .findFirst().orElseThrow();

        // Horário livre: amanhã às 16:00
        LocalDateTime freeSlotStart = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).withHour(16);

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                client.getId(),
                service.getId(),
                freeSlotStart,
                "Agendamento com sucesso em horário livre"
        );

        AppointmentResponse response = appointmentService.create(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(freeSlotStart, response.startDateTime());
        assertEquals(freeSlotStart.plusMinutes(service.getDurationMinutes()), response.endDateTime());
        assertNotNull(response.externalCalendarEventId());
    }
}
