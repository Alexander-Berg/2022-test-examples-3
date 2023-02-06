package ru.yandex.market.promoboss.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.common.postgres.spring.configs.PGCommonConfig;

@Import(PGCommonConfig.class)
public class DbConfiguration {
    @Primary
    @Bean
    public DataSource dataSource(PGCommonConfig pgCommonConfig) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUsername(pgCommonConfig.getUserName());
        dataSource.setPassword( pgCommonConfig.getPassword());
        dataSource.setDriverClassName(pgCommonConfig.getDriverName());
        dataSource.setUrl(pgCommonConfig.getUrl());
        return dataSource;
    }
}
