package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ForbiddenOperationException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.exception.ScheduleConflictException;
import com.app.scheduling.integration.port.CalendarSyncPort;
import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.model.vo.TimeSlot;
import com.app.scheduling.repository.AppointmentRepository;
import com.app.scheduling.service.domain.AppointmentScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private ServiceCatalogService serviceCatalogService;

    @Mock
    private CalendarSyncPort calendarSyncPort;

    @Mock
    private AppointmentScheduleValidator scheduleValidator;

    @InjectMocks
    private AppointmentService appointmentService;

    private User client;
    private User professional;
    private Service service;
    private LocalDateTime futureStart;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(1L)
                .name("Mariana Souza")
                .email("mariana.souza@email.com")
                .phone("(11) 99887-1122")
                .role(UserRole.CLIENT)
                .active(true)
                .build();

        professional = User.builder()
                .id(2L)
                .name("Ana Silva")
                .email("ana.silva@salao.com")
                .phone("(11) 98765-4321")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();

        service = Service.builder()
                .id(10L)
                .name("Manicure Tradicional")
                .description("Cutilagem completa")
                .durationMinutes(30)
                .price(new BigDecimal("45.00"))
                .professional(professional)
                .active(true)
                .build();

        futureStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
    }

    @Nested
    @DisplayName("Cenários de Criação (create)")
    class CreateTests {

        @Test
        @DisplayName("Deve lançar BusinessException quando o serviço está inativo")
        void shouldThrowBusinessExceptionWhenServiceIsInactive() {
            service.setActive(false);
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(serviceCatalogService.getEntityById(service.getId())).thenReturn(service);

            CreateAppointmentRequest request = new CreateAppointmentRequest(client.getId(), service.getId(), futureStart, "Nota");

            BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.create(request));
            assertEquals("O serviço solicitado não está mais ativo para novos agendamentos.", ex.getMessage());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando o profissional está inativo")
        void shouldThrowBusinessExceptionWhenProfessionalIsInactive() {
            professional.setActive(false);
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(serviceCatalogService.getEntityById(service.getId())).thenReturn(service);

            CreateAppointmentRequest request = new CreateAppointmentRequest(client.getId(), service.getId(), futureStart, "Nota");

            BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.create(request));
            assertEquals("O profissional responsável por este serviço está inativo.", ex.getMessage());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar ScheduleConflictException quando há conflito de horário")
        void shouldThrowConflictWhenSlotIsOccupied() {
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(serviceCatalogService.getEntityById(service.getId())).thenReturn(service);
            doThrow(new ScheduleConflictException("Conflito")).when(scheduleValidator).validateNoConflict(eq(professional), any(TimeSlot.class));

            CreateAppointmentRequest request = new CreateAppointmentRequest(client.getId(), service.getId(), futureStart, "Nota");

            assertThrows(ScheduleConflictException.class, () -> appointmentService.create(request));
            verify(calendarSyncPort, never()).createEvent(any());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar agendamento com sucesso com notas e sincronizar com calendário")
        void shouldCreateAppointmentSuccessfullyWithNotes() {
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(serviceCatalogService.getEntityById(service.getId())).thenReturn(service);
            when(calendarSyncPort.createEvent(any(Appointment.class))).thenReturn("mock-cal-123");
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                Appointment a = invocation.getArgument(0);
                a.setId(100L);
                return a;
            });

            CreateAppointmentRequest request = new CreateAppointmentRequest(client.getId(), service.getId(), futureStart, "  Preferência por esmalte nude  ");
            AppointmentResponse response = appointmentService.create(request);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals("Preferência por esmalte nude", response.notes());
            assertEquals("mock-cal-123", response.externalCalendarEventId());
            assertEquals(futureStart, response.startDateTime());
            assertEquals(futureStart.plusMinutes(service.getDurationMinutes()), response.endDateTime());
            verify(scheduleValidator).validateNoConflict(eq(professional), any(TimeSlot.class));
            verify(calendarSyncPort).createEvent(any(Appointment.class));
            verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Deve criar agendamento com sucesso quando notas forem nulas")
        void shouldCreateAppointmentSuccessfullyWithNullNotes() {
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(serviceCatalogService.getEntityById(service.getId())).thenReturn(service);
            when(calendarSyncPort.createEvent(any(Appointment.class))).thenReturn("mock-cal-456");
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                Appointment a = invocation.getArgument(0);
                a.setId(101L);
                return a;
            });

            CreateAppointmentRequest request = new CreateAppointmentRequest(client.getId(), service.getId(), futureStart, null);
            AppointmentResponse response = appointmentService.create(request);

            assertNotNull(response);
            assertNull(response.notes());
            assertEquals(101L, response.id());
        }
    }

    @Nested
    @DisplayName("Cenários de Consulta (findAll, findById, findFutureByClient)")
    class QueryTests {

        @Test
        @DisplayName("Deve listar agendamentos filtrando por professionalId")
        void shouldFindAllByProfessionalId() {
            Appointment a = Appointment.builder().id(1L).client(client).professional(professional).service(service).build();
            when(appointmentRepository.findByProfessionalId(2L)).thenReturn(List.of(a));

            List<AppointmentResponse> result = appointmentService.findAll(2L, null);

            assertEquals(1, result.size());
            verify(appointmentRepository).findByProfessionalId(2L);
            verify(appointmentRepository, never()).findByClientId(anyLong());
            verify(appointmentRepository, never()).findAllWithDetails();
        }

        @Test
        @DisplayName("Deve listar agendamentos filtrando por clientId quando professionalId for nulo")
        void shouldFindAllByClientId() {
            Appointment a = Appointment.builder().id(2L).client(client).professional(professional).service(service).build();
            when(appointmentRepository.findByClientId(1L)).thenReturn(List.of(a));

            List<AppointmentResponse> result = appointmentService.findAll(null, 1L);

            assertEquals(1, result.size());
            verify(appointmentRepository).findByClientId(1L);
            verify(appointmentRepository, never()).findByProfessionalId(anyLong());
        }

        @Test
        @DisplayName("Deve listar todos com detalhes quando nenhum filtro for passado")
        void shouldFindAllWithDetailsWhenNoFilter() {
            Appointment a = Appointment.builder().id(3L).client(client).professional(professional).service(service).build();
            when(appointmentRepository.findAllWithDetails()).thenReturn(List.of(a));

            List<AppointmentResponse> result = appointmentService.findAll(null, null);

            assertEquals(1, result.size());
            verify(appointmentRepository).findAllWithDetails();
        }

        @Test
        @DisplayName("Deve listar agendamentos futuros do cliente validando se o cliente existe")
        void shouldFindFutureAppointmentsByClient() {
            Appointment a = Appointment.builder().id(4L).client(client).professional(professional).service(service).build();
            when(userService.getEntityById(client.getId())).thenReturn(client);
            when(appointmentRepository.findFutureAppointmentsByClient(eq(client.getId()), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(List.of(a));

            List<AppointmentResponse> result = appointmentService.findFutureByClient(client.getId());

            assertEquals(1, result.size());
            verify(userService).getEntityById(client.getId());
        }

        @Test
        @DisplayName("Deve retornar agendamento por ID com sucesso")
        void shouldFindByIdSuccessfully() {
            Appointment a = Appointment.builder().id(5L).client(client).professional(professional).service(service).build();
            when(appointmentRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(a));

            AppointmentResponse response = appointmentService.findById(5L);

            assertNotNull(response);
            assertEquals(5L, response.id());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando agendamento não for encontrado")
        void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
            when(appointmentRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> appointmentService.findById(999L));
        }
    }

    @Nested
    @DisplayName("Cenários de Cancelamento (cancel)")
    class CancelTests {

        @Test
        @DisplayName("Deve chamar cancel(id, null) através do método de conveniência")
        void shouldDelegateCancelWithoutRequester() {
            Appointment a = Appointment.builder().id(10L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(a);

            AppointmentResponse response = appointmentService.cancel(10L);

            assertNotNull(response);
            assertEquals(AppointmentStatus.CANCELLED, a.getStatus());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o ID do agendamento não existir")
        void shouldThrowNotFoundOnCancelUnknownId() {
            when(appointmentRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> appointmentService.cancel(99L, 1L));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenOperationException quando solicitante não for o dono do agendamento")
        void shouldThrowForbiddenWhenCancellingOtherUserAppointment() {
            Appointment a = Appointment.builder().id(11L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(a));

            assertThrows(ForbiddenOperationException.class, () -> appointmentService.cancel(11L, 999L));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando o agendamento já se encontra cancelado")
        void shouldThrowBusinessExceptionWhenAlreadyCancelled() {
            Appointment a = Appointment.builder().id(12L).client(client).professional(professional).service(service).status(AppointmentStatus.CANCELLED).build();
            when(appointmentRepository.findByIdWithDetails(12L)).thenReturn(Optional.of(a));

            assertThrows(BusinessException.class, () -> appointmentService.cancel(12L, client.getId()));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve cancelar e remover do calendário externo quando externalCalendarEventId estiver presente")
        void shouldCancelAndDeleteCalendarEvent() {
            Appointment a = Appointment.builder()
                    .id(13L)
                    .client(client)
                    .professional(professional)
                    .service(service)
                    .status(AppointmentStatus.CONFIRMED)
                    .externalCalendarEventId("cal-event-999")
                    .build();

            when(appointmentRepository.findByIdWithDetails(13L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse response = appointmentService.cancel(13L, client.getId());

            assertNotNull(response);
            assertEquals(AppointmentStatus.CANCELLED, a.getStatus());
            verify(calendarSyncPort).deleteEvent("cal-event-999");
        }

        @Test
        @DisplayName("Deve cancelar sem chamar deleteEvent quando externalCalendarEventId for nulo")
        void shouldCancelWithoutCallingDeleteEventWhenExternalIdIsNull() {
            Appointment a = Appointment.builder()
                    .id(14L)
                    .client(client)
                    .professional(professional)
                    .service(service)
                    .status(AppointmentStatus.SCHEDULED)
                    .externalCalendarEventId(null)
                    .build();

            when(appointmentRepository.findByIdWithDetails(14L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse response = appointmentService.cancel(14L, client.getId());

            assertNotNull(response);
            assertEquals(AppointmentStatus.CANCELLED, a.getStatus());
            verify(calendarSyncPort, never()).deleteEvent(anyString());
        }
    }

    @Nested
    @DisplayName("Cenários de Reagendamento (reschedule)")
    class RescheduleTests {

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao tentar reagendar agendamento inexistente")
        void shouldThrowNotFoundOnReschedule() {
            when(appointmentRepository.findByIdWithDetails(88L)).thenReturn(Optional.empty());

            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(futureStart.plusDays(2), null);
            assertThrows(ResourceNotFoundException.class, () -> appointmentService.reschedule(88L, req, client.getId()));
        }

        @Test
        @DisplayName("Deve lançar ForbiddenOperationException quando outro usuário tentar reagendar")
        void shouldThrowForbiddenOnRescheduleByOtherUser() {
            Appointment a = Appointment.builder().id(20L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(a));

            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(futureStart.plusDays(2), null);
            assertThrows(ForbiddenOperationException.class, () -> appointmentService.reschedule(20L, req, 999L));
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando tentar reagendar agendamento cancelado")
        void shouldThrowBusinessExceptionWhenReschedulingCancelledAppointment() {
            Appointment a = Appointment.builder().id(21L).client(client).professional(professional).service(service).status(AppointmentStatus.CANCELLED).build();
            when(appointmentRepository.findByIdWithDetails(21L)).thenReturn(Optional.of(a));

            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(futureStart.plusDays(2), null);
            BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.reschedule(21L, req, client.getId()));
            assertEquals("Não é permitido reagendar um agendamento cancelado. Crie um novo agendamento.", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando novo horário de início for passado ou agora")
        void shouldThrowBusinessExceptionWhenNewStartIsNotFuture() {
            Appointment a = Appointment.builder().id(22L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(22L)).thenReturn(Optional.of(a));

            LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(pastTime, null);

            BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.reschedule(22L, req, client.getId()));
            assertEquals("A nova data e hora de início devem ser futuras.", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando novo horário de término for anterior ou igual ao início")
        void shouldThrowBusinessExceptionWhenNewEndIsNotAfterNewStart() {
            Appointment a = Appointment.builder().id(23L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(23L)).thenReturn(Optional.of(a));

            LocalDateTime newStart = futureStart.plusDays(2);
            LocalDateTime invalidEnd = newStart.minusMinutes(10);
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(newStart, invalidEnd);

            BusinessException ex = assertThrows(BusinessException.class, () -> appointmentService.reschedule(23L, req, client.getId()));
            assertEquals("A hora de término deve ser posterior à hora de início.", ex.getMessage());
        }

        @Test
        @DisplayName("Deve lançar ScheduleConflictException quando novo horário conflitar com outro agendamento")
        void shouldThrowConflictWhenReschedulingToOccupiedSlot() {
            Appointment a = Appointment.builder().id(24L).client(client).professional(professional).service(service).status(AppointmentStatus.SCHEDULED).build();
            when(appointmentRepository.findByIdWithDetails(24L)).thenReturn(Optional.of(a));

            LocalDateTime newStart = futureStart.plusDays(2);
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(newStart, null);

            doThrow(new ScheduleConflictException("Conflito")).when(scheduleValidator).validateNoConflictExcluding(eq(professional), eq(24L), any(TimeSlot.class));

            assertThrows(ScheduleConflictException.class, () -> appointmentService.reschedule(24L, req, client.getId()));
            verify(calendarSyncPort, never()).updateEvent(anyString(), any());
        }

        @Test
        @DisplayName("Deve reagendar com sucesso calculando automaticamente o horário final e atualizando calendário")
        void shouldRescheduleSuccessfullyWithAutoEndAndCalendarSync() {
            Appointment a = Appointment.builder()
                    .id(25L)
                    .client(client)
                    .professional(professional)
                    .service(service)
                    .status(AppointmentStatus.CONFIRMED)
                    .externalCalendarEventId("cal-reschedule-1")
                    .build();

            when(appointmentRepository.findByIdWithDetails(25L)).thenReturn(Optional.of(a));

            LocalDateTime newStart = futureStart.plusDays(3);
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(newStart, null);

            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse response = appointmentService.reschedule(25L, req, client.getId());

            assertNotNull(response);
            assertEquals(newStart, response.startDateTime());
            assertEquals(newStart.plusMinutes(service.getDurationMinutes()), response.endDateTime());
            verify(scheduleValidator).validateNoConflictExcluding(eq(professional), eq(25L), any(TimeSlot.class));
            verify(calendarSyncPort).updateEvent(eq("cal-reschedule-1"), eq(a));
        }

        @Test
        @DisplayName("Deve reagendar com sucesso sem chamar updateEvent se externalCalendarEventId for nulo")
        void shouldRescheduleSuccessfullyWithoutCalendarEvent() {
            Appointment a = Appointment.builder()
                    .id(26L)
                    .client(client)
                    .professional(professional)
                    .service(service)
                    .status(AppointmentStatus.CONFIRMED)
                    .externalCalendarEventId(null)
                    .build();

            when(appointmentRepository.findByIdWithDetails(26L)).thenReturn(Optional.of(a));

            LocalDateTime newStart = futureStart.plusDays(3);
            LocalDateTime newEnd = newStart.plusHours(1);
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest(newStart, newEnd);

            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse response = appointmentService.reschedule(26L, req, null);

            assertNotNull(response);
            assertEquals(newStart, response.startDateTime());
            assertEquals(newEnd, response.endDateTime());
            verify(calendarSyncPort, never()).updateEvent(anyString(), any());
        }
    }
}
