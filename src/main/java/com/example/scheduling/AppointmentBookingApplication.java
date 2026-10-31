package com.example.scheduling;

import com.example.scheduling.repository.AppointmentRepository;
import com.example.scheduling.repository.InMemoryAppointmentRepository;
import com.example.scheduling.service.AppointmentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AppointmentBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentBookingApplication.class, args);
    }

    @Bean
    AppointmentRepository appointmentRepository() {
        return new InMemoryAppointmentRepository();
    }

    @Bean
    AppointmentService appointmentService(AppointmentRepository repository) {
        return new AppointmentService(repository);
    }
}