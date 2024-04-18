package com.example.servergrpcspring.event;

import net.devh.boot.grpc.server.event.GrpcServerShutdownEvent;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import net.devh.boot.grpc.server.event.GrpcServerTerminatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class MyEventListenerComponent {

    private static final Logger log = LoggerFactory.getLogger(MyEventListenerComponent.class);

    @EventListener
    public void onServerStarted(GrpcServerStartedEvent event) {
        log.info(String.format("gRPC Server started, listening on address: %s, port: %d", event.getAddress(), event.getPort()));

    }

    @EventListener
    public void onServerShutdown(GrpcServerShutdownEvent event) {

        long timestamp = event.getTimestamp();
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());


        log.info(String.format("gRPC Server shutdown %s, at: %s", event.getServer(),dateTime));

    }

    @EventListener
    public void onServerTerminated(GrpcServerTerminatedEvent event) {


        log.info(String.format("gRPC Server terminated %s", event.getServer()));

    }

}