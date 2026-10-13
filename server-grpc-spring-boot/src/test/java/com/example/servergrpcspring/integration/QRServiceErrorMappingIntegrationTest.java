package com.example.servergrpcspring.integration;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc.QRServiceBlockingStub;
import com.example.lib.QRServiceGrpc.QRServiceStub;
import com.example.servergrpcspring.exception.GrpcExceptionAdvice;
import com.example.servergrpcspring.exception.GrpcExceptionMapper;
import com.example.servergrpcspring.service.QRGeneretedService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * With the real {@code @GrpcAdvice} wired in, the unary and the bidirectional streaming QR calls must
 * report the same status and trailers for the same invalid input.
 */
@SpringBootTest(classes = QRServiceErrorMappingIntegrationTest.Config.class, properties = {
        "grpc.server.inProcessName=qr-error-mapping",
        "grpc.server.port=-1",
        "grpc.client.inProcess.address=in-process:qr-error-mapping"
})
@DirtiesContext
class QRServiceErrorMappingIntegrationTest {

    @Configuration
    @ImportAutoConfiguration({
            GrpcServerAutoConfiguration.class,
            GrpcServerFactoryAutoConfiguration.class,
            GrpcAdviceAutoConfiguration.class,
            GrpcClientAutoConfiguration.class})
    static class Config {

        @Bean
        QRGeneretedService qrGeneretedService() {
            return new QRGeneretedService();
        }

        @Bean
        GrpcExceptionAdvice grpcExceptionAdvice() {
            return new GrpcExceptionAdvice();
        }
    }

    @GrpcClient("inProcess")
    private QRServiceBlockingStub blockingStub;

    @GrpcClient("inProcess")
    private QRServiceStub asyncStub;

    @Test
    void unaryAndStreamingMapInvalidTextToTheSameStatusAndTrailers() throws Exception {
        QRCodeRequest invalid = QRCodeRequest.newBuilder().setText("").build();

        StatusRuntimeException unaryError = catchThrowableOfType(
                () -> blockingStub.generateQRCode(invalid), StatusRuntimeException.class);

        AtomicReference<Throwable> streamError = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        StreamObserver<QRCodeRequest> requests = asyncStub.generateQRCodeByStream(new StreamObserver<>() {
            @Override
            public void onNext(QRCodeResponse value) {
            }

            @Override
            public void onError(Throwable t) {
                streamError.set(t);
                done.countDown();
            }

            @Override
            public void onCompleted() {
                done.countDown();
            }
        });
        requests.onNext(invalid);

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(streamError.get()).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException streamingError = (StatusRuntimeException) streamError.get();

        assertThat(unaryError.getStatus().getCode()).isEqualTo(Status.Code.CANCELLED);
        assertThat(streamingError.getStatus().getCode()).isEqualTo(unaryError.getStatus().getCode());
        assertThat(streamingError.getStatus().getDescription()).isEqualTo(unaryError.getStatus().getDescription());
        assertThat(streamingError.getTrailers().get(GrpcExceptionMapper.REASON_KEY))
                .isEqualTo(unaryError.getTrailers().get(GrpcExceptionMapper.REASON_KEY))
                .isNotNull();
        assertThat(streamingError.getTrailers().get(GrpcExceptionMapper.DETAILS_KEY))
                .isEqualTo(unaryError.getTrailers().get(GrpcExceptionMapper.DETAILS_KEY))
                .isNotNull();
    }
}
