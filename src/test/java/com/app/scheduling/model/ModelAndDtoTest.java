package com.app.scheduling.model;

import com.app.scheduling.dto.request.*;
import com.app.scheduling.dto.response.*;
import com.app.scheduling.exception.*;
import com.app.scheduling.model.vo.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelAndDtoTest {

    @Test
    @DisplayName("Appointment: Testar ciclo de vida @PrePersist, construtores e métodos de domínio")
    void shouldTestAppointmentLifecycleAndConstructors() {
        Appointment empty = new Appointment();
        assertNull(empty.getId());
        assertNull(empty.getCreatedAt());
        assertEquals(AppointmentStatus.SCHEDULED, empty.getStatus());

        empty.setStatus(null);
        empty.onCreate();
        assertNotNull(empty.getCreatedAt());
        assertEquals(AppointmentStatus.SCHEDULED, empty.getStatus());

        LocalDateTime now = LocalDateTime.now().minusDays(1);
        empty.setCreatedAt(now);
        empty.setStatus(AppointmentStatus.CONFIRMED);
        empty.onCreate();
        assertEquals(now, empty.getCreatedAt());
        assertEquals(AppointmentStatus.CONFIRMED, empty.getStatus());

        User client = new User(1L, "Client", "client@email.com", "123", UserRole.CLIENT, true);
        User professional = new User(2L, "Pro", "pro@email.com", "456", UserRole.PROFESSIONAL, true);
        Service service = new Service(10L, "Hair", "Desc", 45, new BigDecimal("50.00"), professional, true);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(45);

        Appointment full = new Appointment(100L, client, professional, service, start, end, AppointmentStatus.CONFIRMED, "cal-1", "Note", now);
        assertEquals(100L, full.getId());
        assertEquals(client, full.getClient());
        assertEquals(professional, full.getProfessional());
        assertEquals(service, full.getService());
        assertEquals(start, full.getStartDateTime());
        assertEquals(end, full.getEndDateTime());
        assertEquals(AppointmentStatus.CONFIRMED, full.getStatus());
        assertEquals("cal-1", full.getExternalCalendarEventId());
        assertEquals("Note", full.getNotes());
        assertEquals(now, full.getCreatedAt());

        full.setNotes("Updated Note");
        assertEquals("Updated Note", full.getNotes());

        // Domain methods
        assertEquals(start, full.getTimeSlot().start());
        assertEquals(end, full.getTimeSlot().end());
        assertTrue(full.isOwnedBy(1L));
        assertFalse(full.isOwnedBy(999L));

        // Ownership failure on cancel
        assertThrows(ForbiddenOperationException.class, () -> full.cancel(999L));

        // Reschedule
        TimeSlot newSlot = TimeSlot.of(start.plusDays(1), 45);
        assertThrows(ForbiddenOperationException.class, () -> full.reschedule(newSlot, 999L));
        full.reschedule(newSlot, 1L);
        assertEquals(newSlot.start(), full.getStartDateTime());
        assertEquals(newSlot.end(), full.getEndDateTime());

        // Cancel
        full.cancel(1L);
        assertEquals(AppointmentStatus.CANCELLED, full.getStatus());
        assertThrows(BusinessException.class, () -> full.cancel(1L));
        assertThrows(BusinessException.class, () -> full.reschedule(newSlot, 1L));
    }

    @Test
    @DisplayName("User: Testar construtores, propriedades e métodos de domínio")
    void shouldTestUserConstructorsAndProperties() {
        User u = new User();
        u.setId(5L);
        u.setName("Test User");
        u.setEmail("test@email.com");
        u.setPhone("999");
        u.setRole(UserRole.ADMIN);
        u.setActive(true);

        assertEquals(5L, u.getId());
        assertEquals("Test User", u.getName());
        assertEquals("test@email.com", u.getEmail());
        assertEquals("999", u.getPhone());
        assertEquals(UserRole.ADMIN, u.getRole());
        assertTrue(u.getActive());

        User built = User.builder()
                .id(6L)
                .name("Admin")
                .email("admin@email.com")
                .role(UserRole.ADMIN)
                .build();
        assertTrue(built.getActive());

        // Domain behaviors
        assertFalse(u.isProfessional());
        assertThrows(BusinessException.class, u::ensureProfessional);

        u.setRole(UserRole.PROFESSIONAL);
        assertTrue(u.isProfessional());
        assertDoesNotThrow(u::ensureProfessional);

        assertDoesNotThrow(u::ensureActive);
        u.setActive(false);
        assertThrows(UnauthorizedException.class, u::ensureActive);
    }

    @Test
    @DisplayName("Service: Testar construtores, propriedades e métodos de domínio")
    void shouldTestServiceConstructorsAndProperties() {
        User pro = User.builder().id(1L).active(true).build();
        Service s = new Service();
        s.setId(1L);
        s.setName("Serv");
        s.setDescription("Desc");
        s.setDurationMinutes(30);
        s.setPrice(new BigDecimal("20.00"));
        s.setProfessional(pro);
        s.setActive(true);

        assertEquals(1L, s.getId());
        assertEquals("Serv", s.getName());
        assertEquals("Desc", s.getDescription());
        assertEquals(30, s.getDurationMinutes());
        assertEquals(new BigDecimal("20.00"), s.getPrice());
        assertTrue(s.getActive());

        assertDoesNotThrow(s::ensureAvailableForBooking);

        pro.setActive(false);
        assertThrows(BusinessException.class, s::ensureAvailableForBooking);

        pro.setActive(true);
        s.setActive(false);
        assertThrows(BusinessException.class, s::ensureAvailableForBooking);

        Service built = Service.builder().active(false).build();
        assertFalse(built.getActive());
    }

    @Test
    @DisplayName("Enums: Testar UserRole e AppointmentStatus")
    void shouldTestEnums() {
        assertEquals(UserRole.CLIENT, UserRole.valueOf("CLIENT"));
        assertEquals(3, UserRole.values().length);

        assertEquals(AppointmentStatus.SCHEDULED, AppointmentStatus.valueOf("SCHEDULED"));
        assertEquals(4, AppointmentStatus.values().length);
    }

    @Test
    @DisplayName("DTOs: Testar fromEntity com valores nulos e presentes")
    void shouldTestDtoFromEntityMappings() {
        assertNull(AppointmentResponse.fromEntity(null));
        assertNull(ServiceResponse.fromEntity(null));
        assertNull(UserSummaryResponse.fromEntity(null));

        User user = User.builder().id(1L).name("Mariana").email("mariana@email.com").phone("123").role(UserRole.CLIENT).build();
        UserSummaryResponse userSummary = UserSummaryResponse.fromEntity(user);
        assertNotNull(userSummary);
        assertEquals(1L, userSummary.id());
        assertEquals("Mariana", userSummary.name());

        LoginResponse loginResp = LoginResponse.fromUser(user, "token-123");
        assertEquals("token-123", loginResp.token());
        assertEquals(1L, loginResp.id());

        Service service = Service.builder().id(2L).name("Corte").description("Desc").durationMinutes(30).price(new BigDecimal("50.00")).professional(user).active(true).build();
        ServiceResponse serviceResp = ServiceResponse.fromEntity(service);
        assertNotNull(serviceResp);
        assertEquals(2L, serviceResp.id());
        assertEquals("Corte", serviceResp.name());

        Appointment appointment = Appointment.builder().id(3L).client(user).professional(user).service(service).startDateTime(LocalDateTime.now()).endDateTime(LocalDateTime.now().plusMinutes(30)).status(AppointmentStatus.SCHEDULED).build();
        AppointmentResponse apptResp = AppointmentResponse.fromEntity(appointment);
        assertNotNull(apptResp);
        assertEquals(3L, apptResp.id());
    }

    @Test
    @DisplayName("DTOs: Testar ErrorResponse e Request DTOs")
    void shouldTestErrorResponseAndRequestDtos() {
        ErrorResponse err1 = new ErrorResponse(400, "Bad Request", "Mensagem", "/api/test", List.of("detalhe"));
        assertEquals(400, err1.status());
        assertEquals("Bad Request", err1.error());
        assertEquals("Mensagem", err1.message());
        assertEquals("/api/test", err1.path());
        assertEquals(1, err1.details().size());
        assertNotNull(err1.timestamp());

        ErrorResponse err2 = new ErrorResponse(404, "Not Found", "Mensagem 2", "/api/notfound");
        assertEquals(404, err2.status());
        assertTrue(err2.details().isEmpty());

        CreateAppointmentRequest req1 = new CreateAppointmentRequest(1L, 2L, LocalDateTime.now(), "Notes");
        assertEquals(1L, req1.clientId());

        CreateServiceRequest req2 = new CreateServiceRequest("N", "D", 30, BigDecimal.TEN, 1L);
        assertEquals("N", req2.name());

        CreateUserRequest req3 = new CreateUserRequest("U", "E", "P", UserRole.CLIENT);
        assertEquals("U", req3.name());

        LoginRequest req4 = new LoginRequest("test@email.com");
        assertEquals("test@email.com", req4.email());

        RescheduleAppointmentRequest req5 = new RescheduleAppointmentRequest(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertNotNull(req5.newStartTime());
    }

    @Test
    @DisplayName("Exceptions: Testar construtores das exceções personalizadas")
    void shouldTestExceptionConstructors() {
        assertEquals("Msg", new BusinessException("Msg").getMessage());
        RuntimeException cause = new RuntimeException("Causa");
        BusinessException exWithCause = new BusinessException("Msg", cause);
        assertEquals("Msg", exWithCause.getMessage());
        assertEquals(cause, exWithCause.getCause());

        assertEquals("Msg", new ForbiddenOperationException("Msg").getMessage());
        assertEquals("Msg", new ResourceNotFoundException("Msg").getMessage());
        assertEquals("Msg", new ScheduleConflictException("Msg").getMessage());
        assertEquals("Msg", new UnauthorizedException("Msg").getMessage());
    }
}
