package com.app.scheduling.service.domain;

import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.model.vo.TimeSlot;
import com.app.scheduling.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentScheduleValidatorTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentScheduleValidator validator;

    private User professional;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        professional = User.builder()
                .id(2L)
                .name("Ana Silva")
                .role(UserRole.PROFESSIONAL)
                .build();

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        slot = TimeSlot.of(start, 30);
    }

    @Test
    @DisplayName("Deve validar com sucesso quando não há conflito na criação")
    void shouldPassWhenNoConflictOnCreate() {
        when(appointmentRepository.findConflictingAppointments(eq(2L), eq(slot.start()), eq(slot.end()), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validateNoConflict(professional, slot));
    }

    @Test
    @DisplayName("Deve lançar ScheduleConflictException quando há conflito na criação")
    void shouldThrowWhenConflictExistsOnCreate() {
        when(appointmentRepository.findConflictingAppointments(eq(2L), eq(slot.start()), eq(slot.end()), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of(new Appointment()));

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () -> validator.validateNoConflict(professional, slot));
        assertTrue(ex.getMessage().contains("já possui um agendamento ativo"));
    }

    @Test
    @DisplayName("Deve validar com sucesso excluindo o próprio agendamento no reagendamento")
    void shouldPassWhenNoConflictOnReschedule() {
        when(appointmentRepository.findConflictingAppointmentsExcluding(eq(2L), eq(10L), eq(slot.start()), eq(slot.end()), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validateNoConflictExcluding(professional, 10L, slot));
    }

    @Test
    @DisplayName("Deve lançar ScheduleConflictException quando há conflito com outro agendamento no reagendamento")
    void shouldThrowWhenConflictExistsOnReschedule() {
        when(appointmentRepository.findConflictingAppointmentsExcluding(eq(2L), eq(10L), eq(slot.start()), eq(slot.end()), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(List.of(new Appointment()));

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () -> validator.validateNoConflictExcluding(professional, 10L, slot));
        assertTrue(ex.getMessage().contains("já possui outro agendamento ativo"));
    }
}
