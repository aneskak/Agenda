package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;

import java.util.List;

/**
 * Contrato de caso de uso para orquestração de agendamentos (DIP & Clean Architecture).
 */
public interface AppointmentUseCase {
    AppointmentResponse create(CreateAppointmentRequest request);
    List<AppointmentResponse> findAll(Long professionalId, Long clientId);
    List<AppointmentResponse> findFutureByClient(Long clientId);
    AppointmentResponse findById(Long id);
    AppointmentResponse cancel(Long id);
    AppointmentResponse cancel(Long id, Long requesterUserId);
    AppointmentResponse reschedule(Long id, RescheduleAppointmentRequest request, Long requesterUserId);
}
