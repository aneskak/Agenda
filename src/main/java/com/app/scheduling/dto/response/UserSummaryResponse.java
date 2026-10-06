package com.app.scheduling.dto.response;

import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        String phone,
        UserRole role
) {
    public static UserSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole()
        );
    }
}
