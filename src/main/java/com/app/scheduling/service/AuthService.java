package com.app.scheduling.service;

import com.app.scheduling.dto.request.LoginRequest;
import com.app.scheduling.dto.response.LoginResponse;
import com.app.scheduling.exception.UnauthorizedException;
import com.app.scheduling.model.User;
import com.app.scheduling.model.vo.Email;
import com.app.scheduling.repository.UserRepository;
import com.app.scheduling.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = Email.of(request.email()).value();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas: e-mail não encontrado no sistema."));

        user.ensureActive();

        String token = tokenProvider.generateToken(user);
        log.info("Usuário autenticado com sucesso! ID: {}, Nome: {}, Role: {}", user.getId(), user.getName(), user.getRole());

        return LoginResponse.fromUser(user, token);
    }
}
