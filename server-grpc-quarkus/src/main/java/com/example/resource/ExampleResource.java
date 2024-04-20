package com.example.resource;

import com.example.HelloGrpc;
import com.example.HelloGrpcGrpc;
import com.example.HelloReply;
import com.example.HelloRequest;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.smallrye.mutiny.Uni;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @GrpcClient
    HelloGrpc hello;

    @GrpcClient("hello")
    HelloGrpcGrpc.HelloGrpcBlockingStub blockingHelloService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/mutiny/{name}")
    public Uni<HelloReply> helloMutiny(String name) {

        Metadata extraHeaders = new Metadata();
        extraHeaders.put(Metadata.Key.of("my-header", Metadata.ASCII_STRING_MARSHALLER), "my-custom-header-value");


        HelloGrpc alteredClient = GrpcClientUtils.attachHeaders(hello, extraHeaders);

        return alteredClient.sayHello(HelloRequest.newBuilder().setName(name).build());

    }

    @GET
    @Path("/blocking/{name}")
    public String helloBlocking(String name) {
        return blockingHelloService
                .sayHello(HelloRequest.newBuilder().setName(name).build())
                .getMessage();
    }



}