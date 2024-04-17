package com.example.servergrpcspring.exception;

public class QRGeneratedException extends RuntimeException{

    public QRGeneratedException(String message, Throwable cause) {
        super(message, cause);
    }
}
