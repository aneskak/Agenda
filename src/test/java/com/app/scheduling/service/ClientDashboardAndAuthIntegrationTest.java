package com.app.scheduling.service;

import com.app.scheduling.dto.request.LoginRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.dto.response.LoginResponse;
import com.app.scheduling.exception.ForbiddenOperationException;
import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.exception.UnauthorizedException;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.repository.AppointmentRepository;
import com.app.scheduling.repository.ServiceRepository;
import com.app.scheduling.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ClientDashboardAndAuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Autenticação: Login bem-sucedido com e-mail cadastrado")
    void shouldLoginSuccessfullyWithValidEmail() {
        LoginResponse response = authService.login(new LoginRequest("mariana.souza@email.com"));

        assertNotNull(response);
        assertEquals("mariana.souza@email.com", response.email());
        assertNotNull(response.token());
        assertTrue(response.token().startsWith("mvp-token-"));
    }

    @Test
    @DisplayName("Autenticação: Falha ao tentar login com e-mail não existente (401)")
    void shouldThrowUnauthorizedExceptionForUnknownEmail() {
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("naoexiste@email.com"))
        );
    }

    @Test
    @DisplayName("Dashboard: Consultar agendamentos futuros do cliente (/me)")
    void shouldListFutureAppointmentsForClient() {
        User mariana = userRepository.findByEmail("mariana.souza@email.com").orElseThrow();

        List<AppointmentResponse> myAppointments = appointmentService.findFutureByClient(mariana.getId());

        assertNotNull(myAppointments);
        assertFalse(myAppointments.isEmpty());
        assertTrue(myAppointments.stream().allMatch(a -> a.client().id().equals(mariana.getId())));
    }

    @Test
    @DisplayName("Ownership: Bloquear cancelamento de agendamento de terceiros (403)")
    void shouldBlockCancellationOfOtherUserAppointment() {
        User mariana = userRepository.findByEmail("mariana.souza@email.com").orElseThrow();
        User lucas = userRepository.findByEmail("lucas.pereira@email.com").orElseThrow();

        // Agendamento pertence a Mariana
        Appointment appointment = appointmentRepository.findAllWithDetails().stream()
                .filter(a -> a.getClient().getId().equals(mariana.getId()))
                .findFirst().orElseThrow();

        // Lucas tenta cancelar agendamento da Mariana
        assertThrows(ForbiddenOperationException.class, () ->
                appointmentService.cancel(appointment.getId(), lucas.getId())
        );
    }

    @Test
    @Transactional
    @DisplayName("Reagendamento: Deve reagendar com sucesso em novo horário livre")
    void shouldRescheduleSuccessfullyWhenSlotIsFree() {
        User mariana = userRepository.findByEmail("mariana.souza@email.com").orElseThrow();

        Appointment appointment = appointmentRepository.findAllWithDetails().stream()
                .filter(a -> a.getClient().getId().equals(mariana.getId()))
                .findFirst().orElseThrow();

        LocalDateTime newStart = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS).withHour(11);
        LocalDateTime newEnd = newStart.plusMinutes(appointment.getService().getDurationMinutes());

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(newStart, newEnd);

        AppointmentResponse updated = appointmentService.reschedule(appointment.getId(), request, mariana.getId());

        assertNotNull(updated);
        assertEquals(newStart, updated.startDateTime());
        assertEquals(newEnd, updated.endDateTime());
    }

    @Test
    @DisplayName("Reagendamento: Bloquear reagendamento para horário já ocupado (409)")
    void shouldFailRescheduleWhenNewSlotHasConflict() {
        User mariana = userRepository.findByEmail("mariana.souza@email.com").orElseThrow();
        User lucas = userRepository.findByEmail("lucas.pereira@email.com").orElseThrow();
        Service manicure = serviceRepository.findAll().stream().filter(s -> s.getName().equals("Manicure Tradicional")).findFirst().orElseThrow();

        // Agendamento 1 já existe amanhã às 14:00 (Mariana)
        // Criar Agendamento 2 para Lucas amanhã às 16:00
        LocalDateTime lucasStart = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).withHour(16);
        Appointment lucasAppointment = appointmentRepository.save(Appointment.builder()
                .client(lucas)
                .professional(manicure.getProfessional())
                .service(manicure)
                .startDateTime(lucasStart)
                .endDateTime(lucasStart.plusMinutes(30))
                .status(AppointmentStatus.CONFIRMED)
                .build());

        // Lucas tenta reagendar seu agendamento para 14:00 (onde Mariana já está agendada)
        LocalDateTime conflictingStart = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).withHour(14);
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(conflictingStart, conflictingStart.plusMinutes(30));

        assertThrows(ScheduleConflictException.class, () ->
                appointmentService.reschedule(lucasAppointment.getId(), request, lucas.getId())
        );
    }
}
