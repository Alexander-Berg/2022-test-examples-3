package ru.yandex.market.volva.config;

import java.io.IOException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 25.05.19
 */
@Configuration
@PropertySource({"classpath:/test-app.properties"})
public class EmbeddedPgConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }
}
