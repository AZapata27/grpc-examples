package com.example.clientgrpcspringboot.client;

import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc.MyServiceBlockingStub;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class HelloWordClientService {

    @GrpcClient("myService")
    private MyServiceBlockingStub myServiceStub;

    public String receiveGreeting(String name) {
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();
        return myServiceStub.sayHello(request).getMessage();
    }

}
