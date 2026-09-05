package com.syfe.finance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String rawUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:postgres}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    @Profile("!test")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String url = rawUrl;
        String user = username;
        String pass = password;

        try {
            if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
                URI uri = new URI(url.replace("jdbc:", ""));
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    if (parts.length > 1) {
                        pass = parts[1];
                    }
                }
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String dbName = uri.getPath();
                url = "jdbc:postgresql://" + uri.getHost() + ":" + port + dbName;
                log.info("Configured Render database connection for host: {}, port: {}", uri.getHost(), port);
            }
        } catch (Exception e) {
            log.warn("Using raw datasource url as-is: {}", e.getMessage());
        }

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setDriverClassName(driverClassName);

        return new HikariDataSource(config);
    }
}
