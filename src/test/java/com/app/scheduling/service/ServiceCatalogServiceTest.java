package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateServiceRequest;
import com.app.scheduling.dto.response.ServiceResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ServiceCatalogService serviceCatalogService;

    private User professional;
    private Service service;

    @BeforeEach
    void setUp() {
        professional = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana.silva@salao.com")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();

        service = Service.builder()
                .id(10L)
                .name("Manicure Tradicional")
                .description("Cutilagem e esmaltação")
                .durationMinutes(30)
                .price(new BigDecimal("45.00"))
                .professional(professional)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar serviços por profissional quando professionalId for informado")
    void shouldFindAllByProfessionalId() {
        when(serviceRepository.findByProfessionalIdAndActiveTrue(1L)).thenReturn(List.of(service));

        List<ServiceResponse> result = serviceCatalogService.findAll(1L);

        assertEquals(1, result.size());
        assertEquals("Manicure Tradicional", result.get(0).name());
        verify(serviceRepository).findByProfessionalIdAndActiveTrue(1L);
        verify(serviceRepository, never()).findAllActiveWithProfessional();
    }

    @Test
    @DisplayName("Deve listar todos os serviços ativos quando professionalId for nulo")
    void shouldFindAllActiveWhenProfessionalIdIsNull() {
        when(serviceRepository.findAllActiveWithProfessional()).thenReturn(List.of(service));

        List<ServiceResponse> result = serviceCatalogService.findAll(null);

        assertEquals(1, result.size());
        verify(serviceRepository).findAllActiveWithProfessional();
    }

    @Test
    @DisplayName("Deve retornar serviço por ID com sucesso")
    void shouldFindByIdSuccessfully() {
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(service));

        ServiceResponse response = serviceCatalogService.findById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando serviço não existir")
    void shouldThrowNotFoundWhenServiceDoesNotExist() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceCatalogService.findById(99L));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando usuário associado não for do perfil PROFESSIONAL")
    void shouldThrowBusinessExceptionWhenUserIsNotProfessional() {
        User clientUser = User.builder()
                .id(2L)
                .name("Cliente Lucas")
                .role(UserRole.CLIENT)
                .build();

        when(userService.getEntityById(2L)).thenReturn(clientUser);

        CreateServiceRequest request = new CreateServiceRequest("Corte", "Desc", 30, new BigDecimal("50.00"), 2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceCatalogService.create(request));
        assertEquals("O usuário informado como profissional deve possuir o perfil PROFESSIONAL.", ex.getMessage());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar serviço com sucesso com descrição preenchida")
    void shouldCreateServiceWithDescription() {
        when(userService.getEntityById(1L)).thenReturn(professional);
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> {
            Service s = i.getArgument(0);
            s.setId(20L);
            return s;
        });

        CreateServiceRequest request = new CreateServiceRequest("  Podologia  ", "  Tratamento dos pés  ", 45, new BigDecimal("80.00"), 1L);

        ServiceResponse response = serviceCatalogService.create(request);

        assertNotNull(response);
        assertEquals(20L, response.id());
        assertEquals("Podologia", response.name());
        assertEquals("Tratamento dos pés", response.description());
        assertEquals(45, response.durationMinutes());
        assertEquals(new BigDecimal("80.00"), response.price());
    }

    @Test
    @DisplayName("Deve criar serviço com sucesso com descrição nula")
    void shouldCreateServiceWithNullDescription() {
        when(userService.getEntityById(1L)).thenReturn(professional);
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> {
            Service s = i.getArgument(0);
            s.setId(21L);
            return s;
        });

        CreateServiceRequest request = new CreateServiceRequest("Penteado", null, 60, new BigDecimal("100.00"), 1L);

        ServiceResponse response = serviceCatalogService.create(request);

        assertNotNull(response);
        assertEquals(21L, response.id());
        assertNull(response.description());
    }
}
