package com.udea.innosistemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Clase principal de la aplicación InnoSistemas
 * 
 * Plataforma de Integración y Desarrollo de Software para Estudiantes de Ingeniería de Sistemas
 * Universidad de Antioquia - Facultad de Ingeniería
 * 
 * Esta aplicación facilita la integración práctica de los conocimientos adquiridos por los 
 * estudiantes a lo largo de siete cursos del área de Ingeniería de Software, permitiendo 
 * formar equipos multidisciplinarios y desarrollar productos mínimos viables (MVPs) en un 
 * entorno de desarrollo ágil y colaborativo.
 * 
 * @author Fábrica-Escuela de Software UdeA
 * @version 1.0.0
 * @since 2025
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = "com.udea.innosistemas.config.properties")
public class InnoSistemasApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(InnoSistemasApplication.class);

    public static void main(String[] args) {
        configureSystemProperties();
        SpringApplication application = new SpringApplication(InnoSistemasApplication.class);
        configureApplicationProperties(application);
        application.run(args);
        logApplicationStartup();
    }

    private static void configureSystemProperties() {
        System.setProperty("user.timezone", "America/Bogota");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "true");
        System.setProperty("spring.main.banner-mode", "console");
    }

    private static void configureApplicationProperties(SpringApplication application) {
        application.setRegisterShutdownHook(true);
    }

    private static void logApplicationStartup() {
        LOGGER.info("\n" +
                        "============================================================\n" +
                        "    INNOSISTEMAS - BACKEND API INICIADO EXITOSAMENTE      \n" +
                        "============================================================\n" +
                        "  Universidad de Antioquia - Facultad de Ingeniería       \n" +
                        "  Programa de Ingeniería de Sistemas                      \n" +
                        "  Fábrica-Escuela de Software CodeF@ctory UdeA            \n" +
                        "============================================================\n" +
                        "  Version: 1.0.0                                          \n" +
                        "  Profile: {}\n" +
                        "  Timezone: {}\n" +
                        "============================================================",
                System.getProperty("spring.profiles.active", "default"),
                System.getProperty("user.timezone"));
    }
}
