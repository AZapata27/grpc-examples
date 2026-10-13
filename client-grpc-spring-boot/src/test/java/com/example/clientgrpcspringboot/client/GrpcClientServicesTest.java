package com.example.clientgrpcspringboot.client;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc;
import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Boots the client application against a fake gRPC server running in-process, so the real
 * {@code @GrpcClient} stubs, the global client interceptor and the basic-auth call credentials are used.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "grpc.client.security-grpc-server.address=in-process:client-services-test",
                "grpc.client.QRService.address=in-process:client-services-test",
                "auth.username=user"
        })
@DirtiesContext
class GrpcClientServicesTest {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();

    private static Server server;

    @Autowired
    private HelloWordClientService helloWordClientService;

    @Autowired
    private QRGeneratedClientService qrGeneratedClientService;

    @BeforeAll
    static void startFakeServer() throws Exception {
        ServerInterceptor captureAuthorization = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                lastAuthorization.set(headers.get(AUTHORIZATION));
                return next.startCall(call, headers);
            }
        };

        server = InProcessServerBuilder.forName("client-services-test")
                .directExecutor()
                .addService(ServerInterceptors.intercept(new FakeHelloService(), captureAuthorization))
                .addService(ServerInterceptors.intercept(new FakeQrService(), captureAuthorization))
                .build()
                .start();
    }

    @AfterAll
    static void stopFakeServer() throws InterruptedException {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void resetCapturedHeaders() {
        lastAuthorization.set(null);
    }

    @Test
    void receiveGreetingReturnsServerMessage() {
        assertThat(helloWordClientService.receiveGreeting("Ana")).isEqualTo("Hi Ana");
    }

    @Test
    void callsCarryBasicAuthCredentialsOfConfiguredUser() {
        helloWordClientService.receiveGreeting("Ana");

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:userPassword".getBytes(StandardCharsets.UTF_8));
        assertThat(lastAuthorization.get()).isEqualTo(expected);
    }

    @Test
    void serverErrorStatusIsPropagatedToCaller() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> helloWordClientService.receiveGreeting("unknown"), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void receiveQRGeneratedReturnsImageBytes() {
        ByteString image = qrGeneratedClientService.receiveQRGenerated("payload");

        assertThat(image.toStringUtf8()).isEqualTo("qr:payload");
    }

    @Test
    void receiveQRGeneratedStreamEmitsServerResponses() {
        String first = qrGeneratedClientService.receiveQRGeneratedStream("streamed")
                .blockFirst(Duration.ofSeconds(5));

        assertThat(first).isEqualTo("Received QR Code: qr:streamed");
    }

    /** Fake greeting service: answers "Hi name" or NOT_FOUND for "unknown". */
    static class FakeHelloService extends MyServiceGrpc.MyServiceImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            if ("unknown".equals(request.getName())) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("no such user").asRuntimeException());
                return;
            }
            responseObserver.onNext(HelloReply.newBuilder().setMessage("Hi " + request.getName()).build());
            responseObserver.onCompleted();
        }
    }

    /** Fake QR service: the "image" is just the request text prefixed with "qr:". */
    static class FakeQrService extends QRServiceGrpc.QRServiceImplBase {
        private static QRCodeResponse toResponse(QRCodeRequest request) {
            return QRCodeResponse.newBuilder().setImageData(ByteString.copyFromUtf8("qr:" + request.getText())).build();
        }

        @Override
        public void generateQRCode(QRCodeRequest request, StreamObserver<QRCodeResponse> responseObserver) {
            responseObserver.onNext(toResponse(request));
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<QRCodeRequest> generateQRCodeByStream(StreamObserver<QRCodeResponse> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(QRCodeRequest request) {
                    responseObserver.onNext(toResponse(request));
                }

                @Override
                public void onError(Throwable t) {
                    // client cancelled the stream
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
