package com.app.scheduling.model;

import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.UnauthorizedException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // --- Domain Behaviors (DDD) ---

    public boolean isProfessional() {
        return this.role == UserRole.PROFESSIONAL;
    }

    public void ensureActive() {
        if (!Boolean.TRUE.equals(this.active)) {
            throw new UnauthorizedException("Usuário inativo. Entre em contato com o suporte.");
        }
    }

    public void ensureProfessional() {
        if (!isProfessional()) {
            throw new BusinessException("O usuário informado como profissional deve possuir o perfil PROFESSIONAL.");
        }
    }
}
