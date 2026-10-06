package com.app.scheduling.repository;

import com.app.scheduling.model.Appointment;
import com.app.scheduling.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.id = :id
    """)
    Optional<Appointment> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.professional.id = :professionalId
          AND a.status <> :cancelledStatus
          AND a.startDateTime < :endDateTime
          AND a.endDateTime > :startDateTime
    """)
    List<Appointment> findConflictingAppointments(
            @Param("professionalId") Long professionalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.professional.id = :professionalId
          AND a.id <> :excludeAppointmentId
          AND a.status <> :cancelledStatus
          AND a.startDateTime < :endDateTime
          AND a.endDateTime > :startDateTime
    """)
    List<Appointment> findConflictingAppointmentsExcluding(
            @Param("professionalId") Long professionalId,
            @Param("excludeAppointmentId") Long excludeAppointmentId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        ORDER BY a.startDateTime DESC
    """)
    List<Appointment> findAllWithDetails();

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.professional.id = :professionalId
        ORDER BY a.startDateTime DESC
    """)
    List<Appointment> findByProfessionalId(@Param("professionalId") Long professionalId);

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.client.id = :clientId
        ORDER BY a.startDateTime DESC
    """)
    List<Appointment> findByClientId(@Param("clientId") Long clientId);

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.client
        JOIN FETCH a.professional
        JOIN FETCH a.service
        WHERE a.client.id = :clientId
          AND a.startDateTime >= :now
          AND a.status <> :cancelledStatus
        ORDER BY a.startDateTime ASC
    """)
    List<Appointment> findFutureAppointmentsByClient(
            @Param("clientId") Long clientId,
            @Param("now") LocalDateTime now,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );
}
