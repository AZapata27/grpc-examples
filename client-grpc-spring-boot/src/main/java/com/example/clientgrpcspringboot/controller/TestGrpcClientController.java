package com.example.clientgrpcspringboot.controller;

import com.example.clientgrpcspringboot.client.HelloWordClientService;
import com.example.clientgrpcspringboot.client.QRGeneratedClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
public class TestGrpcClientController {

    @Value("${auth.username}")
    private String username;

    private final HelloWordClientService helloWordClientService;
    private final QRGeneratedClientService qrGeneratedClientService;

    public TestGrpcClientController(HelloWordClientService helloWordClientService,
                                    QRGeneratedClientService qrGeneratedClientService) {
        this.helloWordClientService = helloWordClientService;
        this.qrGeneratedClientService = qrGeneratedClientService;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String printMessage(@RequestParam(defaultValue = "Test Name") final String name) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Input:\n")
                .append("- name: ").append(name).append(" (Changeable via URL param ?name=X)\n")
                .append("Request-Context:\n")
                .append("- auth user: ")
                .append(this.username).append(" (Configure via application.properties)\n")
                .append("gRPC Response:\n")
                .append(this.helloWordClientService.receiveGreeting(name));
        return sb.toString();
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamFluxQRGenerator(@RequestParam(defaultValue = "Test Name") String name) {
        return qrGeneratedClientService.receiveQRGeneratedStream(name);
    }

}