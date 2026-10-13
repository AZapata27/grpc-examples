package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Calls the Quarkus gRPC server through the application's own client bean ("hello-service" channel),
 * covering the full client -> server round trip inside the test instance.
 */
@QuarkusTest
class HelloGrpcClientBeanTest {

    @Inject
    HelloGrpcClientTest helloGrpcClient;

    @Test
    void clientBeanReachesGrpcService() {
        String message = helloGrpcClient.sayHello("Trinity").await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello Trinity!", message);
    }
}
