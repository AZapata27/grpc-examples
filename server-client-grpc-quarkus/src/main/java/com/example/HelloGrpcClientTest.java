package com.example;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class HelloGrpcClientTest {

    @GrpcClient("hello-service")
    HelloGrpc helloGrpc;

    public Uni<String> sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        return helloGrpc.sayHello(request)
                .onItem().transform(HelloReply::getMessage);
    }
}
