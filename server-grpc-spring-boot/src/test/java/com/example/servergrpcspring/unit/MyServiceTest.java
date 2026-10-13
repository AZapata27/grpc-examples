package com.example.servergrpcspring.unit;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.servergrpcspring.service.HelloWordService;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit test: calls the service implementation directly with a recording StreamObserver.
 */
class MyServiceTest {

    private final HelloWordService myService = new HelloWordService();

    @Test
    void testSayHello() throws Exception {

        HelloRequest request = HelloRequest.newBuilder()
                .setName("Test")
                .build();

        StreamRecorder<HelloReply> responseObserver = StreamRecorder.create();
        myService.sayHello(request, responseObserver);

        assertTrue(responseObserver.awaitCompletion(5, TimeUnit.SECONDS), "The call did not terminate in time");

        assertNull(responseObserver.getError());
        List<HelloReply> results = responseObserver.getValues();
        assertEquals(1, results.size());
        HelloReply response = results.getFirst();

        assertEquals(HelloReply.newBuilder()
                .setMessage("Hello Test")
                .build(), response);
    }

}
