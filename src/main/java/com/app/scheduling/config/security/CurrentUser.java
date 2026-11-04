package com.app.scheduling.config.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para resolução declarativa do ID do usuário autenticado a partir de headers ou parâmetros HTTP.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    boolean required() default false;
}
