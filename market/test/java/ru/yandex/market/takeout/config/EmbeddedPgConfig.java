package ru.yandex.market.takeout.config;

import java.io.IOException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({"classpath:test-app.properties"})
public class EmbeddedPgConfig {

    @Bean
    public Object embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }
}
