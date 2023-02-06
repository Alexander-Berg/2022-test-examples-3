package ru.yandex.market.pers.address.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import({
    InternalConfig.class,
    ExternalConfig.DataSyncConfig.class,
    ExternalConfig.BlackboxConfig.class,
    ExternalConfig.TvmProdConfig.class,
    ExternalConfig.GeocoderConfig.class,
    MockConfigurer.GeoExportConfig.class
})
@PropertySource({
    "classpath:/test-application.properties",
    "classpath:/it-application.properties"
})
public class ITConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
