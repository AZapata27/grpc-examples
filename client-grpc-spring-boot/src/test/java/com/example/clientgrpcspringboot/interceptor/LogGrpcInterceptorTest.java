package com.example.clientgrpcspringboot.interceptor;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The logging client interceptor must be transparent: messages and statuses pass through untouched.
 */
class LogGrpcInterceptorTest {

    private Server server;
    private ManagedChannel channel;
    private MyServiceGrpc.MyServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new MyServiceGrpc.MyServiceImplBase() {
                    @Override
                    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
                        if (request.getName().isEmpty()) {
                            responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
                            return;
                        }
                        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        Channel intercepted = ClientInterceptors.intercept(channel, new LogGrpcInterceptor());
        stub = MyServiceGrpc.newBlockingStub(intercepted);
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void forwardsRequestAndResponse() {
        HelloReply reply = stub.sayHello(HelloRequest.newBuilder().setName("Ana").build());

        assertThat(reply.getMessage()).isEqualTo("Hello Ana");
    }

    @Test
    void forwardsErrorStatus() {
        StatusRuntimeException ex = catchThrowableOfType(
                () -> stub.sayHello(HelloRequest.getDefaultInstance()), StatusRuntimeException.class);

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }
}
