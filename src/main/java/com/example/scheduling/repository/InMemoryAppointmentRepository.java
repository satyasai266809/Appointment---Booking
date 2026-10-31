package com.example.scheduling.repository;

import com.example.scheduling.model.Appointment;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> byId = new LinkedHashMap<>();

    @Override
    public void save(Appointment appointment) {
        byId.put(appointment.id(), appointment);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Appointment> findActiveByProvider(String providerId) {
        return byId.values().stream()
                .filter(appointment -> appointment.providerId().equals(providerId) && appointment.isActive())
                .toList();
    }

    @Override
    public Collection<Appointment> findAll() {
        return List.copyOf(byId.values());
    }
}