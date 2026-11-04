package com.app.scheduling.controller;

import com.app.scheduling.config.security.CurrentUser;
import com.app.scheduling.dto.request.CreateAppointmentRequest;
import com.app.scheduling.dto.request.RescheduleAppointmentRequest;
import com.app.scheduling.dto.response.AppointmentResponse;
import com.app.scheduling.dto.response.ErrorResponse;
import com.app.scheduling.service.AppointmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Endpoints para criação, consulta, cancelamento e reagendamento de horários")
public class AppointmentController {

    private final AppointmentUseCase appointmentUseCase;

    @Operation(summary = "Criar novo agendamento", description = "Cria um agendamento validando conflitos de horário e sincronizando com calendário.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: profissional já possui agendamento no horário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Dashboard do Cliente: Meus Agendamentos", description = "Retorna a lista de agendamentos futuros do cliente autenticado via header 'X-User-Id' ou parâmetro 'userId'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de agendamentos do cliente recuperada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @Parameter(hidden = true)
            @CurrentUser(required = true) Long userId
    ) {
        List<AppointmentResponse> list = appointmentUseCase.findFutureByClient(userId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Listar agendamentos", description = "Retorna lista de agendamentos com filtros opcionais por profissional ou cliente.")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos recuperada com sucesso")
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> list(
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) Long clientId
    ) {
        List<AppointmentResponse> list = appointmentUseCase.findAll(professionalId, clientId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Buscar agendamento por ID", description = "Retorna os detalhes de um agendamento específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        AppointmentResponse response = appointmentUseCase.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reagendar horário", description = "Atualiza a data e horário de um agendamento existente, validando conflito com outros agendamentos e checando ownership.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento reagendado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Data inválida ou agendamento já cancelado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não tem permissão para alterar agendamento de terceiros",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Novo horário indisponível para o profissional",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request,
            @Parameter(hidden = true)
            @CurrentUser Long userId
    ) {
        AppointmentResponse response = appointmentUseCase.reschedule(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar agendamento (DELETE)", description = "Altera o status para CANCELLED e desocupa o horário na agenda do profissional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento cancelado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Agendamento já se encontra cancelado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não tem permissão para cancelar agendamento de terceiros",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentResponse> delete(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @CurrentUser Long userId
    ) {
        AppointmentResponse response = appointmentUseCase.cancel(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar agendamento (PATCH)", description = "Altera o status para CANCELLED.")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @CurrentUser Long userId
    ) {
        AppointmentResponse response = appointmentUseCase.cancel(id, userId);
        return ResponseEntity.ok(response);
    }
}
