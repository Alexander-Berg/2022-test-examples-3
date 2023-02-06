package ru.yandex.market.vendors.analytics.core.config;

import java.io.IOException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author ogonek
 */
@Configuration
public class EmbeddedPostgresConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }
}
