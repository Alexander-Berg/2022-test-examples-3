package ru.yandex.market.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * @author fbokovikov
 */
@Configuration
@PropertySource({
        // из mbi-core
        "classpath:common-servant.properties",
        "classpath:common/common-servant.properties",
        // для тестов
        "classpath:functional-test.properties",
})
public class TestPropertiesConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
