package com.app.scheduling.config.security;

import com.app.scheduling.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * HandlerMethodArgumentResolver que injeta o ID do usuário corrente em parâmetros anotados com @CurrentUser.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        String headerVal = webRequest.getHeader("X-User-Id");
        String paramVal = webRequest.getParameter("userId");

        Long userId = null;
        if (headerVal != null && !headerVal.isBlank()) {
            userId = Long.valueOf(headerVal.trim());
        } else if (paramVal != null && !paramVal.isBlank()) {
            userId = Long.valueOf(paramVal.trim());
        }

        if (annotation != null && annotation.required() && userId == null) {
            throw new UnauthorizedException("Identificação do usuário obrigatória. Envie o header 'X-User-Id' ou parâmetro 'userId'.");
        }

        return userId;
    }
}
