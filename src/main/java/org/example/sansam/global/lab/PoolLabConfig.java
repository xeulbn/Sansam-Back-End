package org.example.sansam.global.lab;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@Profile("pool-lab")
public class PoolLabConfig {

    @Bean
    public RequestTrace requestTrace() throws IOException{
        return new RequestTrace();
    }

    @Bean(destroyMethod = "close")
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource labHikariPool(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public MeasuredPool dataSource(
            @Qualifier("labHikariPool") HikariDataSource pool) {
        return new MeasuredPool(pool);
    }

}
