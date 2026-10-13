package com.example.servergrpcspring.integration;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.lib.MyServiceGrpc.MyServiceBlockingStub;
import com.example.servergrpcspring.service.HelloWordService;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Minimal Spring context (only the gRPC server/client auto-configuration and the service) using an
 * in-process server and a {@code @GrpcClient} injected stub.
 */
@SpringBootTest(classes = MyServiceTest.Config.class, properties = {
        "grpc.server.inProcessName=test", // Enable inProcess server
        "grpc.server.port=-1", // Disable external server
        "grpc.client.inProcess.address=in-process:test" // Configure the client to connect to the inProcess server
})
@DirtiesContext // Ensures that the grpc-server is properly shutdown after the test class
class MyServiceTest {

    /**
     * Nested configuration: excluded from the application's component scan (it belongs to a test class),
     * so it does not register a second HelloWordService in other Spring Boot tests.
     */
    @Configuration
    @ImportAutoConfiguration({
            GrpcServerAutoConfiguration.class, // Create required server beans
            GrpcServerFactoryAutoConfiguration.class, // Select server implementation
            GrpcClientAutoConfiguration.class}) // Support @GrpcClient annotation
    static class Config {

        @Bean
        HelloWordService helloWordService() {
            return new HelloWordService();
        }
    }

    @GrpcClient("inProcess")
    private MyServiceBlockingStub myServiceBlockingStub;

    @Test
    void testSayHello() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("test")
                .build();
        HelloReply response = myServiceBlockingStub.sayHello(request);
        assertNotNull(response);
        assertEquals("Hello test", response.getMessage());
    }

}
