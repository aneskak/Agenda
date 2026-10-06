package com.app.scheduling.integration.adapter;

import com.app.scheduling.integration.port.CalendarSyncPort;
import com.app.scheduling.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador Mock para simular sincronizacao de calendario externo durante o MVP sem acoplamento a APIs de terceiros.
 */
@Slf4j
@Component
public class MockCalendarSyncAdapter implements CalendarSyncPort {

    @Override
    public String createEvent(Appointment appointment) {
        String mockEventId = "mock-cal-event-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[MOCK CALENDAR] Evento criado com sucesso no calendario externo! Event ID: {}, Titulo: '{}', Inicio: {}, Fim: {}, Profissional: {}, Cliente: {}",
                mockEventId,
                appointment.getService().getName(),
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getProfessional().getName(),
                appointment.getClient().getName());
        return mockEventId;
    }

    @Override
    public void updateEvent(String externalEventId, Appointment appointment) {
        log.info("[MOCK CALENDAR] Evento atualizado no calendario externo! Event ID: {}, Novo Inicio: {}, Novo Fim: {}",
                externalEventId,
                appointment.getStartDateTime(),
                appointment.getEndDateTime());
    }

    @Override
    public void deleteEvent(String externalEventId) {
        log.info("[MOCK CALENDAR] Evento cancelado/removido do calendario externo! Event ID: {}", externalEventId);
    }
}
