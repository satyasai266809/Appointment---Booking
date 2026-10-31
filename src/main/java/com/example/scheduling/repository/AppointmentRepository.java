package com.example.scheduling.repository;

import com.example.scheduling.model.Appointment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence port. Implementations are not required to be thread-safe; the service serialises access. */
public interface AppointmentRepository {

    void save(Appointment appointment);

    Optional<Appointment> findById(String id);

    List<Appointment> findActiveByProvider(String providerId);

    Collection<Appointment> findAll();
}