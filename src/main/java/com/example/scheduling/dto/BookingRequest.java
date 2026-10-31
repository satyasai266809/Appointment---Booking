package com.example.scheduling.dto;

public record BookingRequest(String providerId, String startUtc, String endUtc) { }