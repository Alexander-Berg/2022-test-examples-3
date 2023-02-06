package ru.yandex.market.antifraud.orders.test.config;

import java.io.IOException;
import java.time.Duration;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.volva.data.DatasourcePack;
import ru.yandex.market.volva.data.DbCredentials;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 25.05.19
 */
@Configuration
@PropertySource({"classpath:/test-app.properties"})
public class EmbeddedPgConfig {

    @Bean
    public Object embeddedPostgres(@Value("${antifraud.orders.jdbc.use.recipe:false}") boolean useRecipe) throws IOException {
        if (useRecipe) {
            return new PgRecipe();
        } else {
            return EmbeddedPostgres.builder()
                    .setPGStartupWait(Duration.ofSeconds(120))
                    .setServerConfig("timezone", "Europe/Moscow")
                    .start();
        }
    }
}
