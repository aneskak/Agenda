package com.app.scheduling.integration.adapter;

import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MockCalendarSyncAdapterTest {

    private MockCalendarSyncAdapter adapter;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        adapter = new MockCalendarSyncAdapter();

        User client = User.builder().id(1L).name("Mariana").build();
        User professional = User.builder().id(2L).name("Ana").build();
        Service service = Service.builder().id(3L).name("Manicure").build();

        appointment = Appointment.builder()
                .client(client)
                .professional(professional)
                .service(service)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusMinutes(30))
                .build();
    }

    @Test
    @DisplayName("Deve gerar um externalEventId com prefixo mock-cal-event-")
    void shouldCreateEventAndReturnMockEventId() {
        String eventId = adapter.createEvent(appointment);

        assertNotNull(eventId);
        assertTrue(eventId.startsWith("mock-cal-event-"));
    }

    @Test
    @DisplayName("Deve atualizar evento sem lançar exceções")
    void shouldUpdateEventWithoutException() {
        assertDoesNotThrow(() -> adapter.updateEvent("mock-cal-event-123", appointment));
    }

    @Test
    @DisplayName("Deve deletar evento sem lançar exceções")
    void shouldDeleteEventWithoutException() {
        assertDoesNotThrow(() -> adapter.deleteEvent("mock-cal-event-123"));
    }
}
