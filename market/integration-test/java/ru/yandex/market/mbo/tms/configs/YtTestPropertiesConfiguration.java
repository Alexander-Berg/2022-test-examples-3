package ru.yandex.market.mbo.tms.configs;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * @author galaev@yandex-team.ru
 * @since 06/12/2018.
 */
@Configuration
public class YtTestPropertiesConfiguration {

    @Bean
    PropertyPlaceholderConfigurer placeholderConfigurer() {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(
            new ClassPathResource("/mbo-tms/yt/yt-test.properties")
        );
        return configurer;
    }
}
