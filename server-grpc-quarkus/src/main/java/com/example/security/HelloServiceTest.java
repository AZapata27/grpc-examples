package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.HelloGrpc;
import com.example.HelloRequest;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class HelloServiceTest implements Greeter {

    @GrpcClient
    HelloGrpc greeterClient;

    @Test
    void shouldReturnHello() {
        Metadata headers = new Metadata();
        headers.put("Authorization", "Basic am9objpqb2hu");
        var client = GrpcClientUtils.attachHeaders(greeterClient, headers);

        CompletableFuture<String> message = new CompletableFuture<>();
        client.sayHello(HelloRequest.newBuilder().setName("Quarkus").build())
                .subscribe().with(reply -> message.complete(reply.getMessage()));
        assertThat(message.get(5, TimeUnit.SECONDS)).isEqualTo("Hello Quarkus");
    }
}