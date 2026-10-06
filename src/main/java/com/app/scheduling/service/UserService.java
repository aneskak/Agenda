package com.app.scheduling.service;

import com.app.scheduling.dto.request.CreateUserRequest;
import com.app.scheduling.dto.response.UserSummaryResponse;
import com.app.scheduling.exception.BusinessException;
import com.app.scheduling.exception.ResourceNotFoundException;
import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import com.app.scheduling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findAll(UserRole role) {
        List<User> users = (role != null)
                ? userRepository.findByRoleAndActiveTrue(role)
                : userRepository.findAll();

        return users.stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse findById(Long id) {
        return UserSummaryResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
    }

    @Transactional
    public UserSummaryResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail: " + request.email());
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .role(request.role())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        return UserSummaryResponse.fromEntity(savedUser);
    }
}
