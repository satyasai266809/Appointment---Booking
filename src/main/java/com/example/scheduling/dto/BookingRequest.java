package com.example.scheduling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingRequest(
	@NotBlank @Size(max = 128) String providerId,
	@NotBlank @Size(max = 64) String startUtc,
	@NotBlank @Size(max = 64) String endUtc) { }