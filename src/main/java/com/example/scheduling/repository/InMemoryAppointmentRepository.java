package com.example.scheduling.repository;

import com.example.scheduling.model.Appointment;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> byId = new LinkedHashMap<>();
    private final Map<String, Map<String, Appointment>> activeByProvider = new HashMap<>();

    @Override
    public synchronized void save(Appointment appointment) {
        Appointment previous = byId.put(appointment.id(), appointment);
        if (previous != null && previous.isActive()) {
            Map<String, Appointment> previousProviderAppointments = activeByProvider.get(previous.providerId());
            previousProviderAppointments.remove(previous.id());
            if (previousProviderAppointments.isEmpty()) {
                activeByProvider.remove(previous.providerId());
            }
        }
        if (appointment.isActive()) {
            activeByProvider.computeIfAbsent(appointment.providerId(), ignored -> new LinkedHashMap<>())
                    .put(appointment.id(), appointment);
        }
    }

    @Override
    public synchronized Optional<Appointment> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public synchronized List<Appointment> findActiveByProvider(String providerId) {
        Map<String, Appointment> appointments = activeByProvider.get(providerId);
        return appointments == null ? List.of() : List.copyOf(appointments.values());
    }

    @Override
    public synchronized Collection<Appointment> findAll() {
        return List.copyOf(byId.values());
    }
}