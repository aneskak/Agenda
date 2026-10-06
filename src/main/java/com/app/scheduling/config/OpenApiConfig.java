package com.app.scheduling.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Springdoc OpenAPI (Swagger).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Agendamentos de Serviços")
                        .version("1.0.0")
                        .description("API REST para gestao de agendamentos e servicos esteticos com suporte a sincronizacao de calendarios externos.")
                        .contact(new Contact()
                                .name("Tech Lead & Equipe de Engenharia")
                                .email("suporte@scheduling.app"))
                        .license(new License().name("Apache 2.0")));
    }
}
