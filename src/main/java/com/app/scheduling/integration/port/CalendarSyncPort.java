package com.app.scheduling.integration.port;

import com.app.scheduling.model.Appointment;

/**
 * Porta de saida (Port) para integracao com provedores de calendario externo (Google Calendar, Outlook, etc.).
 */
public interface CalendarSyncPort {

    /**
     * Cria um evento no calendario externo correspondente ao agendamento.
     *
     * @param appointment O agendamento salvo
     * @return Identificador unico do evento gerado no calendario externo
     */
    String createEvent(Appointment appointment);

    /**
     * Atualiza um evento existente no calendario externo.
     *
     * @param externalEventId Identificador unico do evento externo
     * @param appointment Dados atualizados do agendamento
     */
    void updateEvent(String externalEventId, Appointment appointment);

    /**
     * Remove ou cancela o evento no calendario externo.
     *
     * @param externalEventId Identificador unico do evento externo
     */
    void deleteEvent(String externalEventId);
}
