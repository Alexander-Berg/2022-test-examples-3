package ru.yandex.market.reporting.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import ru.yandex.market.reporting.generator.config.CheckEmbeddedPgProperty;
import ru.yandex.market.stat.conf.ConfigurationHelper;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Slf4j
@Configuration
@Import(HttpServiceConfig.class)
public class IntegrationTestConfig {
    @Bean
    public static Config config() throws IOException, SQLException {
        Config c = ConfigFactory.load("app-integration-tests.properties",
            ConfigParseOptions.defaults().setAllowMissing(true),
            ConfigResolveOptions.defaults().setAllowUnresolved(true))
            .resolve();
        String metadataJdbcUrl = c.getConfig("reporting.metadata.jdbc").getString("url");
        if(metadataJdbcUrl.equals("embedded")) {
            log.info("using embedded pg");
            c = c.withoutPath("reporting.metadata.jdbc.url")
                .withValue(
                    "reporting.metadata.jdbc.url",
                    ConfigValueFactory.fromAnyRef(CheckEmbeddedPgProperty.runEmbeddedPg(metadataJdbcUrl))
                );
        }
        return c;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(Config config) {
        return ConfigurationHelper.createPropertySourcesPlaceholderConfigurer(config);
    }
}
