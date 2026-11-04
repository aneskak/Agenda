package com.app.scheduling.controller;

import com.app.scheduling.dto.request.CreateServiceRequest;
import com.app.scheduling.dto.response.ServiceResponse;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.exception.GlobalExceptionHandler;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.service.ServiceCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServiceCatalogService serviceCatalogService;

    @InjectMocks
    private ServiceController serviceController;

    private ObjectMapper objectMapper;
    private ServiceResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        UserSummaryResponse professional = new UserSummaryResponse(1L, "Ana Silva", "ana@salao.com", "(11) 98765-4321", UserRole.PROFESSIONAL);
        sampleResponse = new ServiceResponse(10L, "Manicure", "Desc", 30, new BigDecimal("45.00"), professional, true);
    }

    @Test
    @DisplayName("GET /api/services - Deve retornar serviços com filtro opcional por profissional")
    void shouldListServices() throws Exception {
        when(serviceCatalogService.findAll(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/services").param("professionalId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Manicure"));

        verify(serviceCatalogService).findAll(1L);
    }

    @Test
    @DisplayName("GET /api/services/{id} - Deve retornar detalhes do serviço")
    void shouldGetServiceById() throws Exception {
        when(serviceCatalogService.findById(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/services/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("POST /api/services - Deve cadastrar novo serviço")
    void shouldCreateService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest("Pedicure", "Desc", 45, new BigDecimal("55.00"), 1L);
        when(serviceCatalogService.create(any(CreateServiceRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }
}
