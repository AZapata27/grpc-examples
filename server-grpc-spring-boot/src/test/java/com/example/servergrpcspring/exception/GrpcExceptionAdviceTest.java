package com.example.servergrpcspring.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the exception-to-status mapping of {@link GrpcExceptionAdvice}.
 */
class GrpcExceptionAdviceTest {

    private static final Metadata.Key<String> REASON = Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> DETAILS = Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER);

    private final GrpcExceptionAdvice advice = new GrpcExceptionAdvice();

    @Test
    void genericExceptionMapsToInternal() {
        RuntimeException cause = new RuntimeException("boom");

        StatusException result = advice.handleException(cause);

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(result.getStatus().getDescription()).startsWith("Internal Server Error");
        assertThat(result.getStatus().getCause()).isSameAs(cause);
        assertThat(result.getTrailers().get(REASON)).contains("unexpected error");
    }

    @Test
    void illegalArgumentMapsToInvalidArgument() {
        Status status = advice.handleInvalidArgument(new IllegalArgumentException("bad"));

        assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(status.getDescription()).isEqualTo("Invalid arguments please check");
    }

    @Test
    void resourceNotFoundMapsToNotFoundWithId() {
        StatusException result = advice.handleResourceNotFoundException(
                new ResourceNotFoundException("missing", null, "42"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(result.getStatus().getDescription()).contains("No resource with id: 42");
        assertThat(result.getTrailers().get(REASON)).isEqualTo("Resource not found");
        assertThat(result.getTrailers().get(DETAILS)).isEqualTo("No resource with id: 42");
    }

    @Test
    void qrGenerationErrorMapsToCancelled() {
        StatusException result = advice.handleQRGeneratedException(
                new QRGeneratedException("cannot render", new IllegalStateException()));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.CANCELLED);
        assertThat(result.getStatus().getDescription()).startsWith("QR Generating Exception");
        assertThat(result.getTrailers().get(DETAILS)).isEqualTo("Please check text");
    }

    @Test
    void authenticationErrorMapsToUnauthenticated() {
        StatusException result = advice.handleAuthenticationCredentialsNotFoundException(
                new BadCredentialsException("bad credentials"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(result.getStatus().getDescription()).startsWith("Authentication Exception");
    }
}
