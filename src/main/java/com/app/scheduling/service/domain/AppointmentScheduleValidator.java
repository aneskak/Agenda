package com.app.scheduling.service.domain;

import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.User;
import com.app.scheduling.model.vo.TimeSlot;
import com.app.scheduling.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Domain Service responsável por validar conflitos e sobreposições de agenda (DDD).
 */
@Component
@RequiredArgsConstructor
public class AppointmentScheduleValidator {

    private final AppointmentRepository appointmentRepository;

    public void validateNoConflict(User professional, TimeSlot slot) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                professional.getId(),
                slot.start(),
                slot.end(),
                AppointmentStatus.CANCELLED
        );

        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(String.format(
                    "O profissional '%s' já possui um agendamento ativo no período selecionado (%s até %s).",
                    professional.getName(),
                    slot.start(),
                    slot.end()
            ));
        }
    }

    public void validateNoConflictExcluding(User professional, Long appointmentId, TimeSlot slot) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentsExcluding(
                professional.getId(),
                appointmentId,
                slot.start(),
                slot.end(),
                AppointmentStatus.CANCELLED
        );

        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(String.format(
                    "O profissional '%s' já possui outro agendamento ativo no novo período solicitado (%s até %s).",
                    professional.getName(),
                    slot.start(),
                    slot.end()
            ));
        }
    }
}
