package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateUserRequest;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve listar usuários por role quando informada")
    void shouldFindAllByRole() {
        User u = User.builder().id(1L).name("Cliente").role(UserRole.CLIENT).build();
        when(userRepository.findByRoleAndActiveTrue(UserRole.CLIENT)).thenReturn(List.of(u));

        List<UserSummaryResponse> result = userService.findAll(UserRole.CLIENT);

        assertEquals(1, result.size());
        verify(userRepository).findByRoleAndActiveTrue(UserRole.CLIENT);
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("Deve listar todos os usuários quando role for nula")
    void shouldFindAllWhenRoleIsNull() {
        User u1 = User.builder().id(1L).name("Cliente").role(UserRole.CLIENT).build();
        User u2 = User.builder().id(2L).name("Pro").role(UserRole.PROFESSIONAL).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserSummaryResponse> result = userService.findAll(null);

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void shouldFindByIdSuccessfully() {
        User u = User.builder().id(1L).name("Lucas").email("lucas@email.com").role(UserRole.CLIENT).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UserSummaryResponse response = userService.findById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Lucas", response.name());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando ID de usuário não existir")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(999L));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando e-mail já estiver em uso")
    void shouldThrowBusinessExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("lucas@email.com")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest("Lucas", "lucas@email.com", "(11) 9999-8888", UserRole.CLIENT);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.create(request));
        assertTrue(ex.getMessage().contains("Já existe um usuário cadastrado com o e-mail"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar novo usuário com telefone preenchido")
    void shouldCreateUserWithPhone() {
        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest("  Novo Usuario  ", "novo@email.com", "  (11) 91111-2222  ", UserRole.CLIENT);

        UserSummaryResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Novo Usuario", response.name());
        assertEquals("novo@email.com", response.email());
        assertEquals("(11) 91111-2222", response.phone());
        assertEquals(UserRole.CLIENT, response.role());
    }

    @Test
    @DisplayName("Deve criar novo usuário com telefone nulo")
    void shouldCreateUserWithNullPhone() {
        when(userRepository.existsByEmail("semfone@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(11L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest("Sem Fone", "semfone@email.com", null, UserRole.PROFESSIONAL);

        UserSummaryResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals(11L, response.id());
        assertNull(response.phone());
    }
}
