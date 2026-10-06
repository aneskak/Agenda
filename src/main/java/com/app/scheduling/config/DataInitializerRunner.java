package com.app.scheduling.config;

import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.AppointmentRepository;
import com.app.scheduling.repository.ServiceRepository;
import com.app.scheduling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Runner responsavel por popular o banco em memoria H2 na inicializacao (Seed de Dados Mockados do MVP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializerRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Banco de dados já contém registros. Pulando inicialização de dados mockados.");
            return;
        }

        log.info("Iniciando carga de dados mockados para o MVP...");

        // 1. Cadastro de Profissionais
        User ana = User.builder()
                .name("Ana Silva - Manicure e Nail Design")
                .email("ana.silva@salao.com")
                .phone("(11) 98765-4321")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();

        User carlos = User.builder()
                .name("Carlos Oliveira - Cabeleireiro")
                .email("carlos.oliveira@salao.com")
                .phone("(11) 91234-5678")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();

        // 2. Cadastro de Clientes
        User mariana = User.builder()
                .name("Mariana Souza")
                .email("mariana.souza@email.com")
                .phone("(11) 99887-1122")
                .role(UserRole.CLIENT)
                .active(true)
                .build();

        User lucas = User.builder()
                .name("Lucas Pereira")
                .email("lucas.pereira@email.com")
                .phone("(11) 97766-3344")
                .role(UserRole.CLIENT)
                .active(true)
                .build();

        userRepository.saveAll(List.of(ana, carlos, mariana, lucas));
        log.info("Usuários mockados cadastrados com sucesso (2 profissionais e 2 clientes).");

        // 3. Cadastro de Serviços
        Service manicure = Service.builder()
                .name("Manicure Tradicional")
                .description("Cutilagem completa, hidratação das mãos e esmaltação tradicional.")
                .durationMinutes(30)
                .price(new BigDecimal("45.00"))
                .professional(ana)
                .active(true)
                .build();

        Service nailDesign = Service.builder()
                .name("Nail Design (Alongamento em Gel)")
                .description("Alongamento de unhas em gel com acabamento natural e cutilagem russa.")
                .durationMinutes(90)
                .price(new BigDecimal("160.00"))
                .professional(ana)
                .active(true)
                .build();

        Service corteCabelo = Service.builder()
                .name("Corte de Cabelo")
                .description("Corte personalizado com lavagem e finalização com pomada modeladora.")
                .durationMinutes(45)
                .price(new BigDecimal("70.00"))
                .professional(carlos)
                .active(true)
                .build();

        Service finalizacao = Service.builder()
                .name("Tratamento e Finalização Capilar")
                .description("Hidratação profunda, nutrição capilar e escova modelada.")
                .durationMinutes(60)
                .price(new BigDecimal("120.00"))
                .professional(carlos)
                .active(true)
                .build();

        serviceRepository.saveAll(List.of(manicure, nailDesign, corteCabelo, finalizacao));
        log.info("Serviços mockados cadastrados com sucesso (4 serviços associados aos profissionais).");

        // 4. Criação de um agendamento pré-existente para testes de conflito
        LocalDateTime sampleStart = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).withHour(14);
        LocalDateTime sampleEnd = sampleStart.plusMinutes(manicure.getDurationMinutes());

        Appointment sampleAppointment = Appointment.builder()
                .client(mariana)
                .professional(ana)
                .service(manicure)
                .startDateTime(sampleStart)
                .endDateTime(sampleEnd)
                .status(AppointmentStatus.CONFIRMED)
                .externalCalendarEventId("mock-cal-seed-001")
                .notes("Cliente prefere tons claros.")
                .build();

        appointmentRepository.save(sampleAppointment);
        log.info("Agendamento de exemplo pré-cadastrado para amanhã das {} às {} (ID: {}).",
                sampleStart, sampleEnd, sampleAppointment.getId());

        log.info("Carga inicial de dados finalizada com sucesso!");
    }
}
