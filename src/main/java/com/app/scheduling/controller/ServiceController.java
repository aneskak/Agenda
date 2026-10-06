package com.app.scheduling.controller;

import com.app.scheduling.dto.request.CreateServiceRequest;
import com.app.scheduling.dto.response.ErrorResponse;
import com.app.scheduling.dto.response.ServiceResponse;
import com.app.scheduling.service.ServiceCatalogService;
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
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Endpoints para consulta e cadastro de servicos oferecidos")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @Operation(summary = "Listar serviços ativos", description = "Retorna os servicos disponiveis, com filtro opcional por ID do profissional.")
    @ApiResponse(responseCode = "200", description = "Lista de servicos retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> list(@RequestParam(required = false) Long professionalId) {
        List<ServiceResponse> services = serviceCatalogService.findAll(professionalId);
        return ResponseEntity.ok(services);
    }

    @Operation(summary = "Buscar serviço por ID", description = "Retorna os detalhes de um servico cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servico encontrado"),
            @ApiResponse(responseCode = "404", description = "Servico nao encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        ServiceResponse response = serviceCatalogService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cadastrar novo serviço", description = "Cadastra um servico associado a um profissional existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servico cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisicao invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profissional nao encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = serviceCatalogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
