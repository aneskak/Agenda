package com.app.scheduling.config;

import com.app.scheduling.SchedulingApplication;
import com.app.scheduling.config.security.CurrentUserArgumentResolver;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.repository.AppointmentRepository;
import com.app.scheduling.repository.ServiceRepository;
import com.app.scheduling.repository.UserRepository;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigAndRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("CorsConfig: Deve registrar mapeamentos de CORS e ArgumentResolvers")
    void shouldConfigureCors() {
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
        CorsConfig corsConfig = new CorsConfig(resolver);
        CorsRegistry registry = new CorsRegistry();

        assertDoesNotThrow(() -> corsConfig.addCorsMappings(registry));
        assertDoesNotThrow(() -> corsConfig.addArgumentResolvers(new ArrayList<>()));
    }

    @Test
    @DisplayName("OpenApiConfig: Deve criar configuração de OpenAPI")
    void shouldCreateOpenApiBean() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertNotNull(openAPI.getInfo().getTitle());
    }

    @Test
    @DisplayName("DataInitializerRunner: Deve pular inicialização quando o banco já possui registros")
    void shouldSkipDataInitializationWhenDatabaseIsNotEmpty() {
        DataInitializerRunner runner = new DataInitializerRunner(userRepository, serviceRepository, appointmentRepository);
        when(userRepository.count()).thenReturn(4L);

        runner.run();

        verify(userRepository, never()).saveAll(anyList());
        verify(serviceRepository, never()).saveAll(anyList());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("DataInitializerRunner: Deve executar carga mockada quando o banco estiver vazio")
    void shouldInitializeDataWhenDatabaseIsEmpty() {
        DataInitializerRunner runner = new DataInitializerRunner(userRepository, serviceRepository, appointmentRepository);
        when(userRepository.count()).thenReturn(0L);

        runner.run();

        verify(userRepository).saveAll(anyList());
        verify(serviceRepository).saveAll(anyList());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("SchedulingApplication: Deve inicializar classe principal e método main")
    void shouldInitializeSchedulingApplication() {
        SchedulingApplication app = new SchedulingApplication();
        assertNotNull(app);

        try (MockedStatic<SpringApplication> mockedSpring = Mockito.mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(eq(SchedulingApplication.class), any(String[].class)))
                    .thenReturn(null);

            SchedulingApplication.main(new String[]{});

            mockedSpring.verify(() -> SpringApplication.run(eq(SchedulingApplication.class), any(String[].class)));
        }
    }
}
