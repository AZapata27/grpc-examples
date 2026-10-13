package com.example.servergrpcspring.service;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc;
import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc;
import com.example.servergrpcspring.exception.GrpcExceptionMapper;
import com.example.servergrpcspring.interceptor.LogGrpcInterceptor;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the plain gRPC service implementations (without Spring) through a real in-process
 * gRPC server, with the server {@link LogGrpcInterceptor} in the call chain.
 */
class GrpcServicesInProcessTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void startServer() throws Exception {
        String name = InProcessServerBuilder.generateName();
        LogGrpcInterceptor logInterceptor = new LogGrpcInterceptor();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(ServerInterceptors.intercept(new HelloWordService(), logInterceptor))
                .addService(ServerInterceptors.intercept(new QRGeneretedService(), logInterceptor))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void stopServer() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void sayHelloReturnsGreeting() {
        HelloReply reply = MyServiceGrpc.newBlockingStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("Ana").build());

        assertThat(reply.getMessage()).isEqualTo("Hello Ana");
    }

    @Test
    void generateQRCodeReturnsPngImage() {
        QRCodeResponse response = QRServiceGrpc.newBlockingStub(channel)
                .generateQRCode(QRCodeRequest.newBuilder().setText("https://grpc.io").build());

        assertIsPng(response.getImageData());
    }

    @Test
    void generateQRCodeWithEmptyTextFailsWithoutAdvice() {
        // Without the Spring @GrpcAdvice the QRGeneratedException surfaces as UNKNOWN.
        assertThatThrownBy(() -> QRServiceGrpc.newBlockingStub(channel)
                .generateQRCode(QRCodeRequest.newBuilder().setText("").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
                .isEqualTo(Status.Code.UNKNOWN);
    }

    @Test
    void generateQRCodeByStreamAnswersEveryRequest() throws Exception {
        List<QRCodeResponse> responses = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        StreamObserver<QRCodeRequest> requests = QRServiceGrpc.newStub(channel)
                .generateQRCodeByStream(new StreamObserver<>() {
                    @Override
                    public void onNext(QRCodeResponse value) {
                        responses.add(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        error.set(t);
                        done.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        done.countDown();
                    }
                });

        requests.onNext(QRCodeRequest.newBuilder().setText("one").build());
        requests.onNext(QRCodeRequest.newBuilder().setText("two").build());
        requests.onNext(QRCodeRequest.newBuilder().setText("three").build());
        requests.onCompleted();

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
        assertThat(responses).hasSize(3);
        responses.forEach(r -> assertIsPng(r.getImageData()));
    }

    @Test
    void generateQRCodeByStreamMapsErrorForEmptyTextLikeTheUnaryAdvice() throws Exception {
        StreamRecorder recorder = new StreamRecorder();
        StreamObserver<QRCodeRequest> requests = QRServiceGrpc.newStub(channel).generateQRCodeByStream(recorder);

        requests.onNext(QRCodeRequest.newBuilder().setText("").build());

        assertThat(recorder.done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(recorder.completed).isFalse();
        assertThat(recorder.responses).isEmpty();
        assertQrGenerationError(recorder.error.get());
    }

    @Test
    void generateQRCodeByStreamAnswersValidMessageThenFailsOnInvalidAndStopsProcessing() throws Exception {
        StreamRecorder recorder = new StreamRecorder();
        StreamObserver<QRCodeRequest> requests = QRServiceGrpc.newStub(channel).generateQRCodeByStream(recorder);

        requests.onNext(QRCodeRequest.newBuilder().setText("valid").build());
        requests.onNext(QRCodeRequest.newBuilder().setText("").build());
        // Messages sent after the server failed the call must not be processed.
        requests.onNext(QRCodeRequest.newBuilder().setText("ignored").build());
        requests.onCompleted();

        assertThat(recorder.done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(recorder.completed).isFalse();
        assertThat(recorder.responses).hasSize(1);
        assertIsPng(recorder.responses.get(0).getImageData());
        assertQrGenerationError(recorder.error.get());
    }

    private static void assertQrGenerationError(Throwable error) {
        assertThat(error).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException statusError = (StatusRuntimeException) error;
        assertThat(statusError.getStatus().getCode()).isEqualTo(Status.Code.CANCELLED);
        assertThat(statusError.getStatus().getDescription()).startsWith("QR Generating Exception");
        assertThat(statusError.getTrailers()).isNotNull();
        assertThat(statusError.getTrailers().get(GrpcExceptionMapper.REASON_KEY))
                .isEqualTo("An error occurred while generating the QR code");
        assertThat(statusError.getTrailers().get(GrpcExceptionMapper.DETAILS_KEY)).isEqualTo("Please check text");
    }

    /** Collects the server responses of a streaming call and signals when it terminates. */
    private static final class StreamRecorder implements StreamObserver<QRCodeResponse> {
        final List<QRCodeResponse> responses = new CopyOnWriteArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        volatile boolean completed;

        @Override
        public void onNext(QRCodeResponse value) {
            responses.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            done.countDown();
        }

        @Override
        public void onCompleted() {
            completed = true;
            done.countDown();
        }
    }

    private static void assertIsPng(ByteString imageData) {
        assertThat(imageData.size()).isGreaterThan(PNG_SIGNATURE.length);
        assertThat(imageData.substring(0, PNG_SIGNATURE.length).toByteArray()).isEqualTo(PNG_SIGNATURE);
    }
}
