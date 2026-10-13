package com.example.clientgrpcspringboot.controller;

import com.example.clientgrpcspringboot.client.HelloWordClientService;
import com.example.clientgrpcspringboot.client.QRGeneratedClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestGrpcClientController.class)
class TestGrpcClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HelloWordClientService helloWordClientService;

    @MockBean
    private QRGeneratedClientService qrGeneratedClientService;

    @Test
    void rootEndpointRendersGrpcGreeting() throws Exception {
        when(helloWordClientService.receiveGreeting("Ana")).thenReturn("Hello Ana");

        mockMvc.perform(get("/").param("name", "Ana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("- name: Ana")))
                .andExpect(content().string(containsString("- auth user: user")))
                .andExpect(content().string(containsString("Hello Ana")));
    }

    @Test
    void rootEndpointUsesDefaultName() throws Exception {
        when(helloWordClientService.receiveGreeting("Test Name")).thenReturn("Hello Test Name");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello Test Name")));
    }
}
