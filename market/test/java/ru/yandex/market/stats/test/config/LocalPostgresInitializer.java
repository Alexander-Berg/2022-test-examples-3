package ru.yandex.market.stats.test.config;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

public class LocalPostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>,
    ApplicationListener<ContextClosedEvent> {

    private EmbeddedPostgres embeddedPostgres;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            embeddedPostgres = new EmbeddedPostgres(Version.V10_6);
            embeddedPostgres.start();

            String url = embeddedPostgres.getConnectionUrl().orElseThrow(IllegalStateException::new);

            ConfigurableEnvironment env = applicationContext.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("embedded-postgres",
                ImmutableMap.of(
                    "embedded-postgres.jdbc.url", url,
                    "embedded-postgres.jdbc.username", "ro",
                    "embedded-postgres.jdbc.password", "",
                    "embedded-postgres.jdbc.driver", org.postgresql.Driver.class.getCanonicalName(),
                    "dictionaries.yt.metadata.jdbc.schema", ""
                )
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        applicationContext.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent ignore) {
        if (embeddedPostgres != null) {
            embeddedPostgres.stop();
        }
    }
}
