package ru.yandex.market.stats.test.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import ru.yandex.market.stat.conf.ConfigurationHelper;

/**
 * @author Ekaterina Lebedeva <kateleb@yandex-team.ru>
 */
@Configuration
@Profile({"integration-tests"})
public class PropertiesITestConfig {

    @Bean
    public static Config config() {
        return ConfigFactory.load(
            "integration-tests.conf",
            ConfigParseOptions.defaults().setAllowMissing(true),
            ConfigResolveOptions.defaults()
        );
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(Config config) {
        return ConfigurationHelper.createPropertySourcesPlaceholderConfigurer(config);
    }
}
