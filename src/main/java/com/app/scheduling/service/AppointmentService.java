package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ForbiddenOperationException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.integration.port.CalendarSyncPort;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final ServiceCatalogService serviceCatalogService;
    private final CalendarSyncPort calendarSyncPort;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        User client = userService.getEntityById(request.clientId());
        Service service = serviceCatalogService.getEntityById(request.serviceId());

        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BusinessException("O serviço solicitado não está mais ativo para novos agendamentos.");
        }

        User professional = service.getProfessional();
        if (!Boolean.TRUE.equals(professional.getActive())) {
            throw new BusinessException("O profissional responsável por este serviço está inativo.");
        }

        LocalDateTime startDateTime = request.startDateTime();
        LocalDateTime endDateTime = startDateTime.plusMinutes(service.getDurationMinutes());

        // Regra 1: Validacao de Conflito de Horario do Profissional
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                professional.getId(),
                startDateTime,
                endDateTime,
                AppointmentStatus.CANCELLED
        );

        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(String.format(
                    "O profissional '%s' já possui um agendamento ativo no período selecionado (%s até %s).",
                    professional.getName(),
                    startDateTime,
                    endDateTime
            ));
        }

        Appointment appointment = Appointment.builder()
                .client(client)
                .professional(professional)
                .service(service)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .build();

        // Integração com Calendário Externo (Ports & Adapters)
        String externalEventId = calendarSyncPort.createEvent(appointment);
        appointment.setExternalCalendarEventId(externalEventId);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso! ID: {}, Cliente: {}, Profissional: {}, Início: {}",
                savedAppointment.getId(), client.getName(), professional.getName(), startDateTime);

        return AppointmentResponse.fromEntity(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll(Long professionalId, Long clientId) {
        List<Appointment> appointments;
        if (professionalId != null) {
            appointments = appointmentRepository.findByProfessionalId(professionalId);
        } else if (clientId != null) {
            appointments = appointmentRepository.findByClientId(clientId);
        } else {
            appointments = appointmentRepository.findAllWithDetails();
        }

        return appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findFutureByClient(Long clientId) {
        // Valida se o cliente existe
        userService.getEntityById(clientId);

        List<Appointment> appointments = appointmentRepository.findFutureAppointmentsByClient(
                clientId,
                LocalDateTime.now(),
                AppointmentStatus.CANCELLED
        );

        return appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        return cancel(id, null);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, Long requesterUserId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        // Regra 6: Verificação de Ownership (o cliente só pode cancelar o seu próprio agendamento)
        if (requesterUserId != null && !appointment.getClient().getId().equals(requesterUserId)) {
            throw new ForbiddenOperationException("Acesso negado: você só tem permissão para cancelar os seus próprios agendamentos.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Este agendamento já se encontra cancelado.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        // Regra 4: Cancelamento e Liberação de Horário na Porta Externa
        if (appointment.getExternalCalendarEventId() != null) {
            calendarSyncPort.deleteEvent(appointment.getExternalCalendarEventId());
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento cancelado com sucesso! ID: {}, Solicitante: {}", id, requesterUserId);

        return AppointmentResponse.fromEntity(updatedAppointment);
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, RescheduleAppointmentRequest request, Long requesterUserId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        // Regra 6: Verificação de Ownership
        if (requesterUserId != null && !appointment.getClient().getId().equals(requesterUserId)) {
            throw new ForbiddenOperationException("Acesso negado: você só tem permissão para reagendar os seus próprios agendamentos.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é permitido reagendar um agendamento cancelado. Crie um novo agendamento.");
        }

        LocalDateTime newStart = request.newStartTime();
        if (!newStart.isAfter(LocalDateTime.now())) {
            throw new BusinessException("A nova data e hora de início devem ser futuras.");
        }

        LocalDateTime newEnd = (request.newEndTime() != null)
                ? request.newEndTime()
                : newStart.plusMinutes(appointment.getService().getDurationMinutes());

        if (!newEnd.isAfter(newStart)) {
            throw new BusinessException("A hora de término deve ser posterior à hora de início.");
        }

        // Regra 7: Validação de conflito excluindo o próprio agendamento
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentsExcluding(
                appointment.getProfessional().getId(),
                appointment.getId(),
                newStart,
                newEnd,
                AppointmentStatus.CANCELLED
        );

        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(String.format(
                    "O profissional '%s' já possui outro agendamento ativo no novo período solicitado (%s até %s).",
                    appointment.getProfessional().getName(),
                    newStart,
                    newEnd
            ));
        }

        appointment.setStartDateTime(newStart);
        appointment.setEndDateTime(newEnd);

        // Notifica sincronização de calendário externo
        if (appointment.getExternalCalendarEventId() != null) {
            calendarSyncPort.updateEvent(appointment.getExternalCalendarEventId(), appointment);
        }

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Agendamento reagendado com sucesso! ID: {}, Novo Início: {}, Novo Fim: {}",
                updated.getId(), newStart, newEnd);

        return AppointmentResponse.fromEntity(updated);
    }
}
