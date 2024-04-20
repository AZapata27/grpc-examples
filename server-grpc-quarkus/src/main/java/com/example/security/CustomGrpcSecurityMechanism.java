package com.example.security;

import io.quarkus.grpc.auth.GrpcSecurityMechanism;
import jakarta.inject.Singleton;

import io.grpc.Metadata;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;

@Singleton
public class CustomGrpcSecurityMechanism implements GrpcSecurityMechanism {

    private static final String AUTHORIZATION = "Authorization";

    @Override
    public boolean handles(Metadata metadata) {
        String authString = metadata.get(Metadata.Key.of(AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER));
        return authString != null && authString.startsWith("Custom ");
    }

    @Override
    public AuthenticationRequest createAuthenticationRequest(Metadata metadata) {
        final String authString = metadata.get(Metadata.Key.of(AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER));
        final String userName = "user";
        final String password = "pass";
        // here comes your application logic that transforms 'authString' to user name and password
        return new UsernamePasswordAuthenticationRequest(userName, new PasswordCredential(password.toCharArray()));
    }
}