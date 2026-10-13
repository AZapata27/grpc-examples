package com.example.servergrpcspring.security;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc;
import com.example.lib.QRCodeRequest;
import com.example.lib.QRServiceGrpc;
import com.example.servergrpcspring.ServerGrpcSpringApplication;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Boots the whole server application (security configuration, interceptors and {@code @GrpcAdvice})
 * on an in-process gRPC server and calls it as an external client would.
 */
@SpringBootTest(
        classes = ServerGrpcSpringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "grpc.server.in-process-name=secured-server-test",
                "grpc.server.port=-1",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        })
@DirtiesContext
class SecuredServerIntegrationTest {

    private static final Metadata.Key<String> REASON = Metadata.Key.of("reason", Metadata.ASCII_STRING_MARSHALLER);

    private ManagedChannel channel;

    @BeforeEach
    void openChannel() {
        channel = InProcessChannelBuilder.forName("secured-server-test").directExecutor().build();
    }

    @AfterEach
    void closeChannel() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    private MyServiceGrpc.MyServiceBlockingStub helloStub(String user, String password) {
        return MyServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(CallCredentialsHelper.basicAuth(user, password));
    }

    private static HelloRequest hello(String name) {
        return HelloRequest.newBuilder().setName(name).build();
    }

    @Test
    void userWithGreetRoleIsGreeted() {
        HelloReply reply = helloStub("user", "userPassword").sayHello(hello("Ana"));

        assertThat(reply.getMessage()).isEqualTo("Hello Ana");
    }

    @Test
    void wrongPasswordIsUnauthenticated() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> helloStub("user", "wrong").sayHello(hello("Ana")), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void unknownUserIsUnauthenticated() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> helloStub("mallory", "malloryPassword").sayHello(hello("Ana")), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void userWithoutGreetRoleIsRejected() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> helloStub("guest", "guestPassword").sayHello(hello("Ana")), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
    }

    @Test
    void anonymousCallToSecuredMethodIsRejected() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> MyServiceGrpc.newBlockingStub(channel).sayHello(hello("Ana")), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void unsecuredQrServiceIsReachableAnonymously() {
        var response = QRServiceGrpc.newBlockingStub(channel)
                .generateQRCode(QRCodeRequest.newBuilder().setText("hello").build());

        assertThat(response.getImageData().isEmpty()).isFalse();
    }

    @Test
    void qrGenerationErrorIsMappedByGrpcAdvice() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> QRServiceGrpc.newBlockingStub(channel)
                        .generateQRCode(QRCodeRequest.newBuilder().setText("").build()),
                StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.CANCELLED);
        assertThat(ex.getStatus().getDescription()).startsWith("QR Generating Exception");
        assertThat(ex.getTrailers().get(REASON)).isEqualTo("An error occurred while generating the QR code");
    }
}
