package com.example;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;


@Path("/test-grpc")
public class TestGrpcResource {

    private final HelloGrpcClientTest helloGrpcClient;

    public TestGrpcResource(HelloGrpcClientTest helloGrpcClient) {
        this.helloGrpcClient = helloGrpcClient;
    }

    @GET
    public Uni<String> hello(String name) {
        return helloGrpcClient.sayHello(name);
    }
}
