package com.udea.innosistemas.controller;

import com.udea.innosistemas.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        reset(authenticationService);
    }

    @Test
    void deberiaRetornarHealthStatusExitosamente() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is running"));
    }

    @Test
    void deberiaRechazarRutaInvalida() throws Exception {
        mockMvc.perform(get("/auth/invalid-endpoint"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deberiaRetornarBadRequestSiLoginEsInvalido() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{}")) //
                .andExpect(status().is4xxClientError());
    }
}
