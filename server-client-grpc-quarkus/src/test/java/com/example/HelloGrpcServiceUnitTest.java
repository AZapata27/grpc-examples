package com.example;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain unit test of the Mutiny service implementation (no Quarkus boot).
 */
class HelloGrpcServiceUnitTest {

    private final HelloGrpcService service = new HelloGrpcService();

    @Test
    void greetsByName() {
        HelloReply reply = service.sayHello(HelloRequest.newBuilder().setName("Morpheus").build())
                .await().atMost(Duration.ofSeconds(1));
        assertEquals("Hello Morpheus!", reply.getMessage());
    }

    @Test
    void handlesEmptyName() {
        HelloReply reply = service.sayHello(HelloRequest.getDefaultInstance())
                .await().atMost(Duration.ofSeconds(1));
        assertEquals("Hello !", reply.getMessage());
    }
}
