package com.app.scheduling.dto.response;

import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;

public record LoginResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        String token
) {
    public static LoginResponse fromUser(User user, String token) {
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
}
