package com.example.clientgrpcspringboot.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcGlobalClientInterceptor
public class LogGrpcInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogGrpcInterceptor.class);

    @Override
    public <REQT, RESPT> ClientCall<REQT, RESPT> interceptCall(
            final MethodDescriptor<REQT, RESPT> method,
            final CallOptions callOptions,
            final Channel next) {

        log.info(method.getFullMethodName());

        log.info(callOptions.toString());

        return next.newCall(method, callOptions);
    }

}