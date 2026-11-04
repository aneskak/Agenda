package com.app.scheduling.repository;

import com.app.scheduling.model.User;
import com.app.scheduling.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleAndActiveTrue(UserRole role);

    List<User> findByRole(UserRole role);

    // VULNERÁVEL A SQL INJECTION:
    // A concatenação direta do parâmetro no SQL permite a injeção de comandos
    @Query(value = "SELECT * FROM users WHERE email = '" + ":email" + "'", nativeQuery = true)
    List<User> findByEmailVulnerableBadQuery(@Param("email") String email);
}
