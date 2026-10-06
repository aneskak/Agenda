package com.app.scheduling.service;

import com.app.scheduling.dto.request.LoginRequest;
import com.app.scheduling.dto.response.LoginResponse;
import com.app.scheduling.exception.UnauthorizedException;
import com.app.scheduling.model.User;
import com.app.scheduling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas: e-mail não encontrado no sistema."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Usuário inativo. Entre em contato com o suporte.");
        }

        // Token simplificado para o MVP
        String tokenPayload = String.format("{\"sub\":\"%s\",\"id\":%d,\"role\":\"%s\",\"iat\":%d}",
                user.getEmail(), user.getId(), user.getRole(), System.currentTimeMillis());
        String token = "mvp-token-" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenPayload.getBytes(StandardCharsets.UTF_8));

        log.info("Usuário autenticado com sucesso! ID: {}, Nome: {}, Role: {}", user.getId(), user.getName(), user.getRole());

        return LoginResponse.fromUser(user, token);
    }
}
