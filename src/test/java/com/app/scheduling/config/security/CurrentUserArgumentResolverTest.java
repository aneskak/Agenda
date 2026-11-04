package com.app.scheduling.config.security;

import com.app.scheduling.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserArgumentResolverTest {

    private CurrentUserArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserArgumentResolver();
    }

    void sampleMethod(@CurrentUser(required = true) Long requiredUser,
                      @CurrentUser(required = false) Long optionalUser,
                      String otherParam) {}

    @Test
    @DisplayName("Deve suportar parâmetros anotados com @CurrentUser do tipo Long")
    void shouldSupportAnnotatedLongParameters() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", Long.class, Long.class, String.class);
        MethodParameter paramRequired = new MethodParameter(method, 0);
        MethodParameter paramOptional = new MethodParameter(method, 1);
        MethodParameter paramOther = new MethodParameter(method, 2);

        assertTrue(resolver.supportsParameter(paramRequired));
        assertTrue(resolver.supportsParameter(paramOptional));
        assertFalse(resolver.supportsParameter(paramOther));
    }

    @Test
    @DisplayName("Deve resolver ID a partir do header X-User-Id")
    void shouldResolveFromHeader() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", Long.class, Long.class, String.class);
        MethodParameter param = new MethodParameter(method, 0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object resolved = resolver.resolveArgument(param, null, webRequest, null);

        assertEquals(42L, resolved);
    }

    @Test
    @DisplayName("Deve resolver ID a partir do parâmetro userId se header não existir")
    void shouldResolveFromParameter() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", Long.class, Long.class, String.class);
        MethodParameter param = new MethodParameter(method, 1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", "99");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object resolved = resolver.resolveArgument(param, null, webRequest, null);

        assertEquals(99L, resolved);
    }

    @Test
    @DisplayName("Deve retornar nulo se não obrigatório e nenhum valor informado")
    void shouldReturnNullWhenOptionalAndMissing() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", Long.class, Long.class, String.class);
        MethodParameter param = new MethodParameter(method, 1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object resolved = resolver.resolveArgument(param, null, webRequest, null);

        assertNull(resolved);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException se obrigatório e nenhum valor informado")
    void shouldThrowUnauthorizedWhenRequiredAndMissing() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", Long.class, Long.class, String.class);
        MethodParameter param = new MethodParameter(method, 0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(request);

        assertThrows(UnauthorizedException.class, () -> resolver.resolveArgument(param, null, webRequest, null));
    }
}
