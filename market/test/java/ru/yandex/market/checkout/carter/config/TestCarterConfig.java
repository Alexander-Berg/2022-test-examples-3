package ru.yandex.market.checkout.carter.config;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.checkout.carter.Main;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
@Configuration
@Import({
        Main.class,
        CarterClientConfig.class,
        CarterMocksConfig.class
})
@TestPropertySource({
        "classpath:checkout-storage.properties",
        "classpath:carter-client.properties"
})
public class TestCarterConfig {

    @Nonnull
    @Bean
    public PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertyPlaceholderConfigurer.setOrder(1);
        return propertyPlaceholderConfigurer;
    }
}
