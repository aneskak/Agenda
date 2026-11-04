package com.app.scheduling.security;

import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base64TokenProviderTest {

    @Test
    @DisplayName("Base64TokenProvider: Deve gerar token válido com prefixo mvp-token-")
    void shouldGenerateValidToken() {
        Base64TokenProvider provider = new Base64TokenProvider();
        User user = User.builder()
                .id(1L)
                .name("Mariana")
                .email("mariana@email.com")
                .role(UserRole.CLIENT)
                .build();

        String token = provider.generateToken(user);

        assertNotNull(token);
        assertTrue(token.startsWith("mvp-token-"));
    }
}
