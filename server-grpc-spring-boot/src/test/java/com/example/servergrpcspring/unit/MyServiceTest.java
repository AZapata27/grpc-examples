package com.example.servergrpcspring.unit;

import com.example.lib.HelloReply;
import com.example.lib.HelloRequest;
import com.example.servergrpcspring.service.HelloWordService;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@SpringJUnitConfig(classes = { MyServiceUnitTestConfiguration.class })
class MyServiceTest {

    @Autowired
    private HelloWordService myService;

    @Test
    void testSayHello() throws Exception {

        HelloRequest request = HelloRequest.newBuilder()
                .setName("Test")
                .build();

        StreamRecorder<HelloReply> responseObserver = StreamRecorder.create();
        myService.sayHello(request, responseObserver);

        if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
            fail("The call did not terminate in time");
        }

        assertNull(responseObserver.getError());
        List<HelloReply> results = responseObserver.getValues();
        assertEquals(1, results.size());
        HelloReply response = results.getFirst();

        assertEquals(HelloReply.newBuilder()
                .setMessage("Hello Test")
                .build(), response);
    }

}