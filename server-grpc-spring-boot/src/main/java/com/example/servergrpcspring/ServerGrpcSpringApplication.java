package com.example.servergrpcspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ServerGrpcSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerGrpcSpringApplication.class, args);
    }

}
