package com.app.scheduling.model;

import com.app.scheduling.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // --- Domain Behaviors (DDD) ---

    public void ensureAvailableForBooking() {
        if (!Boolean.TRUE.equals(this.active)) {
            throw new BusinessException("O serviço solicitado não está mais ativo para novos agendamentos.");
        }
        if (this.professional != null && !Boolean.TRUE.equals(this.professional.getActive())) {
            throw new BusinessException("O profissional responsável por este serviço está inativo.");
        }
    }
}
