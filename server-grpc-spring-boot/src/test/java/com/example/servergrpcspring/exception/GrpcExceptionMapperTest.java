package com.example.servergrpcspring.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GrpcExceptionMapper#map(Throwable)} must pick the same mapping the advice would pick by type.
 */
class GrpcExceptionMapperTest {

    @Test
    void mapsEachApplicationExceptionToItsStatus() {
        assertThat(code(new QRGeneratedException("qr", null))).isEqualTo(Status.Code.CANCELLED);
        assertThat(code(new ResourceNotFoundException("missing", null, "1"))).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(code(new BadCredentialsException("bad"))).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(code(new IllegalArgumentException("bad"))).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(code(new IllegalStateException("boom"))).isEqualTo(Status.Code.INTERNAL);
    }

    @Test
    void keepsTrailersAndCause() {
        QRGeneratedException cause = new QRGeneratedException("qr", null);

        StatusException result = GrpcExceptionMapper.map(cause);

        assertThat(result.getStatus().getCause()).isSameAs(cause);
        assertThat(result.getTrailers().get(GrpcExceptionMapper.REASON_KEY))
                .isEqualTo("An error occurred while generating the QR code");
        assertThat(result.getTrailers().get(GrpcExceptionMapper.DETAILS_KEY)).isEqualTo("Please check text");
    }

    @Test
    void passesThroughExistingGrpcStatuses() {
        Metadata trailers = new Metadata();
        trailers.put(GrpcExceptionMapper.REASON_KEY, "already mapped");

        StatusException fromRuntime = GrpcExceptionMapper.map(Status.PERMISSION_DENIED.asRuntimeException(trailers));
        StatusException checked = Status.ABORTED.asException();

        assertThat(fromRuntime.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(fromRuntime.getTrailers().get(GrpcExceptionMapper.REASON_KEY)).isEqualTo("already mapped");
        assertThat(GrpcExceptionMapper.map(checked)).isSameAs(checked);
    }

    private static Status.Code code(Throwable t) {
        return GrpcExceptionMapper.map(t).getStatus().getCode();
    }
}
