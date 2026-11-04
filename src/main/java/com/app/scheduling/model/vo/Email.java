package com.app.scheduling.model.vo;

import com.app.scheduling.exception.BusinessException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object imutável que representa um endereço de e-mail válido e normalizado.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email(String value) {
        Objects.requireNonNull(value, "O e-mail não pode ser nulo.");
        String normalized = value.trim().toLowerCase();
        if (normalized.isBlank() || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("Formato de e-mail inválido: " + value);
        }
        this.value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
