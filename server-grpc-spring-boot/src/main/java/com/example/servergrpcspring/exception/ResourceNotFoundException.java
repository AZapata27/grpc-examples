package com.example.servergrpcspring.exception;

public class ResourceNotFoundException extends RuntimeException {

    private String id;

    public ResourceNotFoundException(String message, Throwable cause, String id) {
        super(message, cause);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
