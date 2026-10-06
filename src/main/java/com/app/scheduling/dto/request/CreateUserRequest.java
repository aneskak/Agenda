package com.app.scheduling.dto.request;

import com.app.scheduling.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome não pode exceder 255 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        String phone,

        @NotNull(message = "O perfil do usuário (role) é obrigatório")
        UserRole role
) {}
