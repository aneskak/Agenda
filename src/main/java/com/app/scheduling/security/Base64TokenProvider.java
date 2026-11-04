package com.app.scheduling.security;

import com.app.scheduling.model.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementação padrão de TokenProvider utilizando Base64 para o MVP.
 */
@Component
public class Base64TokenProvider implements TokenProvider {

    @Override
    public String generateToken(User user) {
        String tokenPayload = String.format("{\"sub\":\"%s\",\"id\":%d,\"role\":\"%s\",\"iat\":%d}",
                user.getEmail(), user.getId(), user.getRole(), System.currentTimeMillis());
        return "mvp-token-" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenPayload.getBytes(StandardCharsets.UTF_8));
    }
}
