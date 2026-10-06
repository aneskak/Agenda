package com.app.scheduling.controller;

import com.app.scheduling.dto.request.CreateUserRequest;
import com.app.scheduling.dto.response.ErrorResponse;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de clientes e profissionais")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuários", description = "Retorna usuarios cadastrados com filtro opcional por perfil (CLIENT, PROFESSIONAL).")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> list(@RequestParam(required = false) UserRole role) {
        List<UserSummaryResponse> users = userService.findAll(role);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna os detalhes de um usuario especifico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> getById(@PathVariable Long id) {
        UserSummaryResponse response = userService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cadastrar usuário", description = "Cadastra um novo cliente ou profissional no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou e-mail duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserSummaryResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserSummaryResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
