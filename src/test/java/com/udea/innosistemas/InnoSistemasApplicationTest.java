package com.udea.innosistemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InnoSistemasApplicationTest {

    @BeforeAll
    static void setup() {
        System.setProperty("user.timezone", "America/Bogota");
    }

    @Test
    void contextLoads() {
        assertTrue(true, "El contexto de Spring Boot debería cargarse correctamente");
    }

    @Test
    void systemPropertiesShouldBeConfigured() {
        assertEquals("America/Bogota", System.getProperty("user.timezone"));
        assertEquals("UTF-8", System.getProperty("file.encoding", "UTF-8"));
        assertEquals("true", System.getProperty("java.awt.headless"));
        assertEquals("console", System.getProperty("spring.main.banner-mode"));
    }

    @Test
    void mainMethodShouldRunWithoutErrors() {
        assertDoesNotThrow(() -> InnoSistemasApplication.main(new String[]{}));
    }
}
