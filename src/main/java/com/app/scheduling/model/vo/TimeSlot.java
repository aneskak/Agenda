package com.app.scheduling.model.vo;

import com.app.scheduling.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object imutável que representa um intervalo de tempo para agendamento.
 * Encapsula validações de integridade temporal e regras de sobreposição.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "A data e hora de início são obrigatórias.");
        Objects.requireNonNull(end, "A data e hora de término são obrigatórias.");

        if (!end.isAfter(start)) {
            throw new BusinessException("A hora de término deve ser posterior à hora de início.");
        }
    }

    public static TimeSlot of(LocalDateTime start, int durationMinutes) {
        Objects.requireNonNull(start, "A data e hora de início são obrigatórias.");
        if (durationMinutes <= 0) {
            throw new BusinessException("A duração deve ser positiva.");
        }
        return new TimeSlot(start, start.plusMinutes(durationMinutes));
    }

    public static TimeSlot of(LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(start, end);
    }

    public boolean isFuture() {
        return start.isAfter(LocalDateTime.now());
    }

    public void validateIsFuture() {
        if (!isFuture()) {
            throw new BusinessException("A nova data e hora de início devem ser futuras.");
        }
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    public boolean overlaps(TimeSlot other) {
        Objects.requireNonNull(other, "O intervalo para comparação não pode ser nulo.");
        return this.start.isBefore(other.end) && this.end.isAfter(other.start);
    }
}
