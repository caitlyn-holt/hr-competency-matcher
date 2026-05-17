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
            String jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%d/%s?ssl=true&sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory",
                    dbHost, dbPort, dbName
            );

            ds.setJdbcUrl(jdbcUrl);
            ds.setUsername(dbUser);
            ds.setPassword(dbPassword);
            ds.setDriverClassName("org.postgresql.Driver");

            // Оптимизация пула для Render Free tier
            ds.setMaximumPoolSize(5);
            ds.setMinimumIdle(1);
            ds.setConnectionTimeout(30000);

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