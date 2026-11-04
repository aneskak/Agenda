package com.app.scheduling.model.vo;

import com.app.scheduling.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsTest {

    @Test
    @DisplayName("TimeSlot: Deve criar com sucesso intervalo válido e calcular duração")
    void shouldCreateValidTimeSlot() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(45);

        TimeSlot slot = TimeSlot.of(start, end);

        assertNotNull(slot);
        assertEquals(start, slot.start());
        assertEquals(end, slot.end());
        assertEquals(45, slot.durationMinutes());
        assertTrue(slot.isFuture());
        assertDoesNotThrow(slot::validateIsFuture);
    }

    @Test
    @DisplayName("TimeSlot: Deve criar intervalo a partir de início e duração em minutos")
    void shouldCreateTimeSlotFromDuration() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        TimeSlot slot = TimeSlot.of(start, 30);

        assertEquals(start, slot.start());
        assertEquals(start.plusMinutes(30), slot.end());
        assertEquals(30, slot.durationMinutes());
    }

    @Test
    @DisplayName("TimeSlot: Deve lançar BusinessException para duração inválida")
    void shouldThrowWhenDurationIsInvalid() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        assertThrows(BusinessException.class, () -> TimeSlot.of(start, 0));
        assertThrows(BusinessException.class, () -> TimeSlot.of(start, -10));
    }

    @Test
    @DisplayName("TimeSlot: Deve lançar BusinessException quando término for anterior ou igual ao início")
    void shouldThrowWhenEndIsNotAfterStart() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThrows(BusinessException.class, () -> TimeSlot.of(start, start));
        assertThrows(BusinessException.class, () -> TimeSlot.of(start, start.minusMinutes(10)));
    }

    @Test
    @DisplayName("TimeSlot: Deve lançar BusinessException ao validar se início é futuro quando for passado")
    void shouldThrowWhenValidatingPastTimeSlot() {
        LocalDateTime past = LocalDateTime.now().minusHours(2);
        TimeSlot slot = new TimeSlot(past, past.plusMinutes(30));

        assertFalse(slot.isFuture());
        BusinessException ex = assertThrows(BusinessException.class, slot::validateIsFuture);
        assertEquals("A nova data e hora de início devem ser futuras.", ex.getMessage());
    }

    @Test
    @DisplayName("TimeSlot: Deve verificar sobreposição (overlap) corretamente")
    void shouldCheckOverlapsCorrectly() {
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        TimeSlot slot1 = TimeSlot.of(base, 60); // 10:00 - 11:00

        TimeSlot overlapping1 = TimeSlot.of(base.plusMinutes(30), 60); // 10:30 - 11:30
        TimeSlot overlapping2 = TimeSlot.of(base.minusMinutes(30), 60); // 09:30 - 10:30
        TimeSlot nonOverlapping = TimeSlot.of(base.plusMinutes(60), 30); // 11:00 - 11:30

        assertTrue(slot1.overlaps(overlapping1));
        assertTrue(slot1.overlaps(overlapping2));
        assertFalse(slot1.overlaps(nonOverlapping));
    }

    @Test
    @DisplayName("Email: Deve criar e normalizar e-mail válido")
    void shouldCreateAndNormalizeValidEmail() {
        Email email = Email.of("  Mariana.Souza@Email.COM  ");

        assertEquals("mariana.souza@email.com", email.value());
        assertEquals("mariana.souza@email.com", email.toString());
    }

    @Test
    @DisplayName("Email: Deve lançar BusinessException para formato inválido de e-mail")
    void shouldThrowForInvalidEmailFormat() {
        assertThrows(BusinessException.class, () -> Email.of("invalido"));
        assertThrows(BusinessException.class, () -> Email.of("   "));
        assertThrows(NullPointerException.class, () -> Email.of(null));
    }
}
