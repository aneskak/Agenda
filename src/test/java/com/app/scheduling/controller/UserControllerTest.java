package com.app.scheduling.controller;

import com.app.scheduling.dto.request.CreateUserRequest;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.exception.GlobalExceptionHandler;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private UserSummaryResponse sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleUser = new UserSummaryResponse(1L, "Lucas Pereira", "lucas@email.com", "(11) 97766-3344", UserRole.CLIENT);
    }

    @Test
    @DisplayName("GET /api/users - Deve listar usuários com filtro opcional por perfil")
    void shouldListUsers() throws Exception {
        when(userService.findAll(UserRole.CLIENT)).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/api/users").param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Lucas Pereira"));

        verify(userService).findAll(UserRole.CLIENT);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Deve buscar usuário por ID")
    void shouldGetUserById() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleUser);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /api/users - Deve cadastrar novo usuário")
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Novo Cliente", "novo@email.com", "(11) 98888-7777", UserRole.CLIENT);
        when(userService.create(any(CreateUserRequest.class))).thenReturn(sampleUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
