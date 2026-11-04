package com.app.scheduling.exception;

import com.app.scheduling.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    @DisplayName("Deve tratar ScheduleConflictException retornando 409 CONFLICT")
    void shouldHandleScheduleConflict() {
        ScheduleConflictException ex = new ScheduleConflictException("Horário ocupado");

        ResponseEntity<ErrorResponse> response = handler.handleScheduleConflict(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Horário ocupado", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar UnauthorizedException retornando 401 UNAUTHORIZED")
    void shouldHandleUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Credenciais inválidas");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Credenciais inválidas", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar ForbiddenOperationException retornando 403 FORBIDDEN")
    void shouldHandleForbidden() {
        ForbiddenOperationException ex = new ForbiddenOperationException("Acesso negado");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Acesso negado", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar ResourceNotFoundException retornando 404 NOT_FOUND")
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Não encontrado");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Não encontrado", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar BusinessException retornando 400 BAD_REQUEST")
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException("Regra de negócio violada");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Regra de negócio violada", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar Exception genérica retornando 500 INTERNAL_SERVER_ERROR")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Erro inesperado no servidor");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertTrue(response.getBody().message().contains("Ocorreu um erro interno no servidor"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException cobrindo FieldError e ObjectError")
    void shouldHandleValidationErrorsWithFieldAndGlobalErrors() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "email", "Formato de e-mail inválido"));
        bindingResult.addError(new ObjectError("target", "Objeto com estado inválido global"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1
        );

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals(2, response.getBody().details().size());
        assertTrue(response.getBody().details().contains("email: Formato de e-mail inválido"));
        assertTrue(response.getBody().details().contains("Objeto com estado inválido global"));
    }
}
