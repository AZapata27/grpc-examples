package com.example.servergrpcspring.unit;

import com.example.servergrpcspring.service.HelloWordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyServiceUnitTestConfiguration {


    @Bean
    HelloWordService myService() {
        return new HelloWordService();
    }

}