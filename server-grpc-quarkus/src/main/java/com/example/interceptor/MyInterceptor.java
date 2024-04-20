package com.example.interceptor;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

@GlobalInterceptor 
@ApplicationScoped
public class MyInterceptor implements ClientInterceptor {

    private static final Logger logger = Logger.getLogger(MyInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {

        logger.info("Interceptando llamada a: " + method.getFullMethodName());

        return next.newCall(method, callOptions);
    }
}