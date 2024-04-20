package com.example.service;

import com.example.HelloGrpc;
import com.example.HelloReply;
import com.example.HelloRequest;
import io.quarkus.grpc.GrpcService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;

@GrpcService
public class HelloGrpcService implements HelloGrpc {


    @RolesAllowed("admin")
    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom()
                .item("Hello " + request.getName() + "!")
                .map(msg -> HelloReply.newBuilder().setMessage(msg).build());
    }

}
