package com.example.scheduling.service;

public record CancelResult(Status status, String message) {

    public enum Status { CANCELLED, NOT_FOUND, ALREADY_CANCELLED, INVALID_INPUT }

    static CancelResult of(Status status, String message) {
        return new CancelResult(status, message);
    }
}