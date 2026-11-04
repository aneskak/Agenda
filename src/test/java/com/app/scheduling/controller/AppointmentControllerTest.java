package com.app.scheduling.controller;

import com.app.scheduling.config.security.CurrentUserArgumentResolver;
import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.dto.response.ServiceResponse;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.exception.GlobalExceptionHandler;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.service.AppointmentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentUseCase appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    private ObjectMapper objectMapper;
    private AppointmentResponse sampleResponse;
    private LocalDateTime futureStart;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        futureStart = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime futureEnd = futureStart.plusMinutes(30);

        UserSummaryResponse client = new UserSummaryResponse(1L, "Mariana", "mariana@email.com", "(11) 9999-1111", UserRole.CLIENT);
        UserSummaryResponse professional = new UserSummaryResponse(2L, "Ana", "ana@salao.com", "(11) 8888-2222", UserRole.PROFESSIONAL);
        ServiceResponse service = new ServiceResponse(10L, "Manicure", "Desc", 30, new BigDecimal("45.00"), professional, true);

        sampleResponse = new AppointmentResponse(
                100L,
                client,
                professional,
                service,
                futureStart,
                futureEnd,
                AppointmentStatus.SCHEDULED,
                "mock-cal-123",
                "Observação",
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/appointments - Deve retornar 201 Created quando requisição for válida")
    void shouldCreateAppointment() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(1L, 10L, futureStart, "Observação");
        when(appointmentService.create(any(CreateAppointmentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.notes").value("Observação"));
    }

    @Test
    @DisplayName("GET /api/appointments/me - Deve retornar 200 OK via Header X-User-Id")
    void shouldGetMyAppointmentsViaHeader() throws Exception {
        when(appointmentService.findFutureByClient(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments/me")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L));

        verify(appointmentService).findFutureByClient(1L);
    }

    @Test
    @DisplayName("GET /api/appointments/me - Deve retornar 200 OK via Param userId")
    void shouldGetMyAppointmentsViaParam() throws Exception {
        when(appointmentService.findFutureByClient(2L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments/me")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L));

        verify(appointmentService).findFutureByClient(2L);
    }

    @Test
    @DisplayName("GET /api/appointments/me - Deve retornar 401 quando nem header nem param forem informados")
    void shouldReturn401WhenNoUserIdProvidedForMe() throws Exception {
        mockMvc.perform(get("/api/appointments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/appointments - Deve listar com filtros opcionais")
    void shouldListAppointmentsWithFilters() throws Exception {
        when(appointmentService.findAll(2L, 1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments")
                        .param("professionalId", "2")
                        .param("clientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - Deve retornar agendamento por ID")
    void shouldGetById() throws Exception {
        when(appointmentService.findById(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/appointments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id}/reschedule - Deve reagendar com sucesso")
    void shouldReschedule() throws Exception {
        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(futureStart.plusDays(1), futureStart.plusDays(1).plusMinutes(30));
        when(appointmentService.reschedule(eq(100L), any(RescheduleAppointmentRequest.class), eq(1L)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/appointments/100/reschedule")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} - Deve cancelar agendamento")
    void shouldDeleteAppointment() throws Exception {
        when(appointmentService.cancel(100L, 1L)).thenReturn(sampleResponse);

        mockMvc.perform(delete("/api/appointments/100")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{id}/cancel - Deve cancelar via PATCH")
    void shouldCancelAppointmentViaPatch() throws Exception {
        when(appointmentService.cancel(100L, 1L)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/appointments/100/cancel")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }
}
