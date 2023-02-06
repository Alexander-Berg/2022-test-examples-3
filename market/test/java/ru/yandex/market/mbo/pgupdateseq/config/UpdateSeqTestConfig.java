package ru.yandex.market.mbo.pgupdateseq.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class UpdateSeqTestConfig {
    private final DbConfig dbConfig;

    public UpdateSeqTestConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

}
