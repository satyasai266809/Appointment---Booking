package com.example.scheduling;

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