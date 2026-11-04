package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.integration.port.CalendarSyncPort;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.model.vo.TimeSlot;
import com.app.scheduling.repository.AppointmentRepository;
import com.app.scheduling.service.domain.AppointmentScheduleValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService implements AppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final ServiceCatalogService serviceCatalogService;
    private final CalendarSyncPort calendarSyncPort;
    private final AppointmentScheduleValidator scheduleValidator;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        User client = userService.getEntityById(request.clientId());
        Service service = serviceCatalogService.getEntityById(request.serviceId());

        service.ensureAvailableForBooking();

        User professional = service.getProfessional();
        TimeSlot slot = TimeSlot.of(request.startDateTime(), service.getDurationMinutes());

        // Validação de conflito de horário delegada ao Domain Service
        scheduleValidator.validateNoConflict(professional, slot);

        Appointment appointment = Appointment.builder()
                .client(client)
                .professional(professional)
                .service(service)
                .startDateTime(slot.start())
                .endDateTime(slot.end())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .build();

        // Integração com Calendário Externo (Ports & Adapters)
        String externalEventId = calendarSyncPort.createEvent(appointment);
        appointment.setExternalCalendarEventId(externalEventId);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso! ID: {}, Cliente: {}, Profissional: {}, Início: {}",
                savedAppointment.getId(), client.getName(), professional.getName(), slot.start());

        return AppointmentResponse.fromEntity(savedAppointment);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findFutureByClient(Long clientId) {
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

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id) {
        return cancel(id, null);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id, Long requesterUserId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        // Regra de cancelamento e ownership encapsulada na entidade (Tell, Don't Ask)
        appointment.cancel(requesterUserId);

        // Notifica porta externa
        if (appointment.getExternalCalendarEventId() != null) {
            calendarSyncPort.deleteEvent(appointment.getExternalCalendarEventId());
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento cancelado com sucesso! ID: {}, Solicitante: {}", id, requesterUserId);

        return AppointmentResponse.fromEntity(updatedAppointment);
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(Long id, RescheduleAppointmentRequest request, Long requesterUserId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        LocalDateTime newStart = request.newStartTime();
        TimeSlot newSlot = (request.newEndTime() != null)
                ? TimeSlot.of(newStart, request.newEndTime())
                : TimeSlot.of(newStart, appointment.getService().getDurationMinutes());

        // Regra de ownership, validação temporal e transição de estado na entidade (Tell, Don't Ask)
        appointment.reschedule(newSlot, requesterUserId);

        // Validação de conflito de agenda delegada ao Domain Service
        scheduleValidator.validateNoConflictExcluding(appointment.getProfessional(), appointment.getId(), newSlot);

        // Notifica sincronização de calendário externo
        if (appointment.getExternalCalendarEventId() != null) {
            calendarSyncPort.updateEvent(appointment.getExternalCalendarEventId(), appointment);
        }

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Agendamento reagendado com sucesso! ID: {}, Novo Início: {}, Novo Fim: {}",
                updated.getId(), newSlot.start(), newSlot.end());

        return AppointmentResponse.fromEntity(updated);
    }
}
