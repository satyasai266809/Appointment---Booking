package com.example.scheduling.dto;

import com.example.scheduling.model.Appointment;

public record BookingResponse(String status, Appointment appointment, String message) { }