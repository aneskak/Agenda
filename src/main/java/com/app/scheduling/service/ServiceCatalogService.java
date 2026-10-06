package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateServiceRequest;
import com.app.scheduling.dto.response.ServiceResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.model.Service;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ServiceResponse> findAll(Long professionalId) {
        List<Service> services = (professionalId != null)
                ? serviceRepository.findByProfessionalIdAndActiveTrue(professionalId)
                : serviceRepository.findAllActiveWithProfessional();

        return services.stream()
                .map(ServiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        return ServiceResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Service getEntityById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        User professional = userService.getEntityById(request.professionalId());

        if (professional.getRole() != UserRole.PROFESSIONAL) {
            throw new BusinessException("O usuário informado como profissional deve possuir o perfil PROFESSIONAL.");
        }

        Service service = Service.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .professional(professional)
                .active(true)
                .build();

        Service savedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(savedService);
    }
}
