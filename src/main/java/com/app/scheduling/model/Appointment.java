package com.app.scheduling.model;

import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ForbiddenOperationException;
import com.app.scheduling.model.vo.TimeSlot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "external_calendar_event_id")
    private String externalCalendarEventId;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = AppointmentStatus.SCHEDULED;
        }
    }

    // --- Domain Behaviors (DDD - Rich Domain Model & Tell, Don't Ask) ---

    public TimeSlot getTimeSlot() {
        return TimeSlot.of(this.startDateTime, this.endDateTime);
    }

    public boolean isOwnedBy(Long userId) {
        return client != null && Objects.equals(client.getId(), userId);
    }

    public void cancel(Long requesterUserId) {
        if (requesterUserId != null && !isOwnedBy(requesterUserId)) {
            throw new ForbiddenOperationException("Acesso negado: você só tem permissão para cancelar os seus próprios agendamentos.");
        }
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Este agendamento já se encontra cancelado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void reschedule(TimeSlot newSlot, Long requesterUserId) {
        if (requesterUserId != null && !isOwnedBy(requesterUserId)) {
            throw new ForbiddenOperationException("Acesso negado: você só tem permissão para reagendar os seus próprios agendamentos.");
        }
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é permitido reagendar um agendamento cancelado. Crie um novo agendamento.");
        }
        newSlot.validateIsFuture();
        this.startDateTime = newSlot.start();
        this.endDateTime = newSlot.end();
    }
}
