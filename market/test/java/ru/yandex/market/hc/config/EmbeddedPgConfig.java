package ru.yandex.market.hc.config;

import java.io.IOException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by aproskriakov on 11/2/21
 */
@Configuration
@PropertySource({"classpath:/test-app.properties"})
public class EmbeddedPgConfig {

    @Bean
    public Object embeddedPostgres(@Value("${antifraud.orders.jdbc.use.recipe:false}") boolean useRecipe) throws IOException {
        if (useRecipe) {
            return new PgRecipe();
        }
        return EmbeddedPostgres.builder().start();
    }
}
