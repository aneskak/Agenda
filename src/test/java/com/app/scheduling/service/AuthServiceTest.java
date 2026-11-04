package com.app.scheduling.service;

import com.app.scheduling.dto.request.LoginRequest;
import com.app.scheduling.dto.response.LoginResponse;
import com.app.scheduling.exception.UnauthorizedException;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.UserRepository;
import com.app.scheduling.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o e-mail não existir")
    void shouldThrowUnauthorizedWhenEmailNotFound() {
        when(userRepository.findByEmail("desconhecido@email.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("desconhecido@email.com");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Credenciais inválidas: e-mail não encontrado no sistema.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o usuário estiver inativo")
    void shouldThrowUnauthorizedWhenUserIsInactive() {
        User inactiveUser = User.builder()
                .id(1L)
                .name("Inativo")
                .email("inativo@email.com")
                .active(false)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findByEmail("inativo@email.com")).thenReturn(Optional.of(inactiveUser));

        LoginRequest request = new LoginRequest("INATIVO@EMAIL.COM ");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Usuário inativo. Entre em contato com o suporte.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve efetuar login com sucesso, normalizando e-mail e gerando token")
    void shouldLoginSuccessfully() {
        User activeUser = User.builder()
                .id(2L)
                .name("Mariana Souza")
                .email("mariana.souza@email.com")
                .active(true)
                .role(UserRole.CLIENT)
                .build();

        when(userRepository.findByEmail("mariana.souza@email.com")).thenReturn(Optional.of(activeUser));
        when(tokenProvider.generateToken(activeUser)).thenReturn("mvp-token-xyz");

        LoginRequest request = new LoginRequest("  MARIANA.SOUZA@EMAIL.COM  ");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("Mariana Souza", response.name());
        assertEquals("mariana.souza@email.com", response.email());
        assertEquals(UserRole.CLIENT, response.role());
        assertNotNull(response.token());
        assertEquals("mvp-token-xyz", response.token());
        verify(tokenProvider).generateToken(activeUser);
    }
}
