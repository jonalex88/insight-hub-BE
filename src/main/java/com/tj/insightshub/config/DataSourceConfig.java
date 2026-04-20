package com.tj.insightshub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "DATABASE_URL")
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) throws Exception {
        String normalized = databaseUrl
                .replace("postgresql://", "http://")
                .replace("postgres://", "http://");
        URI uri = new URI(normalized);
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 5432;
        String db = uri.getPath().substring(1);
        String[] userInfo = uri.getUserInfo().split(":", 2);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=require");
        config.setUsername(userInfo[0]);
        config.setPassword(userInfo[1]);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        return new HikariDataSource(config);
    }
}
