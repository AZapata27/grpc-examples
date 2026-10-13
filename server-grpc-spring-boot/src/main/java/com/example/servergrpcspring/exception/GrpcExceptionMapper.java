package com.example.servergrpcspring.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.springframework.security.core.AuthenticationException;

/**
 * Single source of truth for translating application exceptions into gRPC {@link Status} + trailers.
 * <p>
 * Used by {@link GrpcExceptionAdvice} for exceptions thrown by unary calls, and directly by streaming
 * observers, whose errors are reported through {@code responseObserver.onError(...)} and therefore never
 * reach the {@code @GrpcAdvice}.
 */
public final class GrpcExceptionMapper {

    public static final Metadata.Key<String> REASON_KEY = Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> DETAILS_KEY = Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcExceptionMapper() {
    }

    /**
     * Maps any throwable to the same status the {@link GrpcExceptionAdvice} would produce for it.
     * Throwables that already carry a gRPC status are passed through unchanged.
     */
    public static StatusException map(Throwable t) {
        if (t instanceof StatusException statusException) {
            return statusException;
        }
        if (t instanceof StatusRuntimeException statusRuntimeException) {
            return statusRuntimeException.getStatus().asException(statusRuntimeException.getTrailers());
        }
        if (t instanceof QRGeneratedException e) {
            return qrGenerated(e);
        }
        if (t instanceof ResourceNotFoundException e) {
            return resourceNotFound(e);
        }
        if (t instanceof AuthenticationException e) {
            return unauthenticated(e);
        }
        if (t instanceof IllegalArgumentException e) {
            return invalidArgument(e).asException();
        }
        return internal(t);
    }

    public static StatusException internal(Throwable e) {
        String details = "Please review traces, logs and contact the system administrator";
        return Status.INTERNAL.withDescription("Internal Server Error")
                .augmentDescription(details)
                .withCause(e)
                .asException(trailers("An unexpected error occurred in the application processing the request", details));
    }

    public static Status invalidArgument(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription("Invalid arguments please check").withCause(e);
    }

    public static StatusException resourceNotFound(ResourceNotFoundException e) {
        String details = String.format("No resource with id: %s", e.getId());
        return Status.NOT_FOUND.withDescription("Resource not found")
                .augmentDescription(details)
                .withCause(e)
                .asException(trailers("Resource not found", details));
    }

    public static StatusException qrGenerated(QRGeneratedException e) {
        String details = "Please check text";
        return Status.CANCELLED.withDescription("QR Generating Exception")
                .augmentDescription(details)
                .withCause(e)
                .asException(trailers("An error occurred while generating the QR code", details));
    }

    public static StatusException unauthenticated(AuthenticationException e) {
        String details = "Please check your autentication credentials";
        return Status.UNAUTHENTICATED.withDescription("Authentication Exception")
                .augmentDescription(details)
                .withCause(e)
                .asException(trailers("An Authentication object was not found in the SecurityContext", details));
    }

    private static Metadata trailers(String reason, String details) {
        Metadata metadata = new Metadata();
        metadata.put(REASON_KEY, reason);
        metadata.put(DETAILS_KEY, details);
        return metadata;
    }
}
