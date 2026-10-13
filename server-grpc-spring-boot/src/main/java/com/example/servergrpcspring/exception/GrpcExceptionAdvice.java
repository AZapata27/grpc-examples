package com.example.servergrpcspring.exception;

import io.grpc.Status;
import io.grpc.StatusException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.core.AuthenticationException;

/**
 * Maps exceptions thrown by gRPC handlers to statuses. The mapping itself lives in
 * {@link GrpcExceptionMapper} so streaming observers (which report errors via {@code onError}
 * and bypass this advice) produce exactly the same status and trailers.
 */
@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(Exception.class)
    StatusException handleException(Exception e) {
        return GrpcExceptionMapper.internal(e);
    }

    @GrpcExceptionHandler
    public Status handleInvalidArgument(IllegalArgumentException e) {
        return GrpcExceptionMapper.invalidArgument(e);
    }

    @GrpcExceptionHandler(ResourceNotFoundException.class)
    public StatusException handleResourceNotFoundException(ResourceNotFoundException e) {
        return GrpcExceptionMapper.resourceNotFound(e);
    }

    @GrpcExceptionHandler(QRGeneratedException.class)
    public StatusException handleQRGeneratedException(QRGeneratedException e) {
        return GrpcExceptionMapper.qrGenerated(e);
    }

    @GrpcExceptionHandler(AuthenticationException.class)
    public StatusException handleAuthenticationCredentialsNotFoundException(AuthenticationException e) {
        return GrpcExceptionMapper.unauthenticated(e);
    }
}
