package com.example.servergrpcspring.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;

@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(Exception.class)
    StatusException handleException(Exception e) {

        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER), "An unexpected error occurred in the application processing the request");
        metadata.put(Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER), "Please review traces, logs and contact the system administrator");

        return Status.INTERNAL.withDescription("Internal Server Error")
                .augmentDescription("Please review traces, logs and contact the system administrator")
                .withCause(e)
                .asException(metadata);

    }

    @GrpcExceptionHandler
    public Status handleInvalidArgument(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription("Invalid arguments please check").withCause(e);
    }

    @GrpcExceptionHandler(ResourceNotFoundException.class)
    public StatusException handleResourceNotFoundException(ResourceNotFoundException e) {

        String details= String.format("No resource with id: %s", e.getId());

        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER), "Resource not found");
        metadata.put(Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER),  details);

        return Status.NOT_FOUND.withDescription("Resource not found")
                .augmentDescription(details)
                .withCause(e)
                .asException(metadata);
    }

    @GrpcExceptionHandler(QRGeneratedException.class)
    public StatusException handleQRGeneratedException(QRGeneratedException e) {

        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER), "An error occurred while generating the QR code");
        metadata.put(Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER), "Please check text");

        return Status.CANCELLED.withDescription("QR Generating Exception")
                .augmentDescription("Please check text")
                .withCause(e)
                .asException(metadata);
    }

    @GrpcExceptionHandler(AuthenticationException.class)
    public StatusException handleAuthenticationCredentialsNotFoundException(AuthenticationException e) {

        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER), "An Authentication object was not found in the SecurityContext");
        metadata.put(Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER), "Please check your autentication credentials");

        return Status.UNAUTHENTICATED.withDescription("Authentication Exception")
                .augmentDescription("Please check your autentication credentials")
                .withCause(e)
                .asException(metadata);

    }


}