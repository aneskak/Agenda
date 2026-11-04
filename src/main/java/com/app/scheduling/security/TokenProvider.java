package com.app.scheduling.security;

import com.app.scheduling.model.User;

/**
 * Strategy interface para geração de tokens de autenticação (DIP & OCP).
 */
public interface TokenProvider {
    String generateToken(User user);
}
