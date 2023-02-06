package ru.yandex.market.mbo.pglogid.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class LogIdTestConfig {
    private final DbConfig dbConfig;

    public LogIdTestConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

}
