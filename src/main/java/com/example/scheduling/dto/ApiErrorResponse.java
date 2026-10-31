package com.example.scheduling.dto;

import java.util.Map;

public record ApiErrorResponse(String status, String message, Map<String, String> errors) { }