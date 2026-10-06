package com.app.scheduling.repository;

import com.app.scheduling.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    @Query("SELECT s FROM Service s JOIN FETCH s.professional WHERE s.active = true")
    List<Service> findAllActiveWithProfessional();

    @Query("SELECT s FROM Service s JOIN FETCH s.professional WHERE s.professional.id = :professionalId AND s.active = true")
    List<Service> findByProfessionalIdAndActiveTrue(@Param("professionalId") Long professionalId);
}
