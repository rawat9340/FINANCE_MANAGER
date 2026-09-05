package com.syfe.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Finance Manager API")
                        .description("Production-quality RESTful backend for Syfe Personal Finance Manager technical assessment. Features session-based authentication, transaction CRUD, custom/default categories, goal tracking, and financial reporting.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Syfe Engineering Candidate")
                                .email("candidate@syfe.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://spring.io")));
    }
}
