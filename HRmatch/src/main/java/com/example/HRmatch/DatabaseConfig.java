package com.example.HRmatch;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_NAME:testdb}")
    private String dbName;

    @Value("${DB_USER:sa}")
    private String dbUser;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${USE_POSTGRES:false}")
    private boolean usePostgres;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();

        if (usePostgres) {
            // Формат с параметрами: надёжно работает с любыми паролями
            String url = String.format(
                    "jdbc:postgresql://%s:%d/%s?user=%s&password=%s&sslmode=require",
                    dbHost, dbPort, dbName, dbUser, dbPassword
            );
            ds.setJdbcUrl(url);
            ds.setDriverClassName("org.postgresql.Driver");
        } else {
            // Локальная H2 база
            ds.setJdbcUrl("jdbc:h2:mem:testdb");
            ds.setDriverClassName("org.h2.Driver");
            ds.setUsername("sa");
            ds.setPassword("");
        }
        return ds;
    }
}