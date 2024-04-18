package com.example.clientgrpcspringboot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.CallCredentials;
import net.devh.boot.grpc.client.inject.StubTransformer;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;

/**
 * The security configuration for the client. In this case we assume that we use the same passwords for all stubs. If
 * you need per stub credentials you can delete the grpcCredentials and define a {@link StubTransformer} yourself.
 *
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Value("${auth.username}")
    private String username;

    @Bean
    // Create credentials for username + password.
    CallCredentials grpcCredentials() {
        return CallCredentialsHelper.basicAuth(this.username, this.username + "Password");
    }

}