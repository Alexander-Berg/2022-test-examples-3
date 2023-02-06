package ru.yandex.market.adv.shop.integration.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.adv.shop.integration.ShopIntegrationTestRunner;

/**
 * Тестовая конфигурация для adv-shop-integration.
 * Date: 27.05.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@Configuration
public class ShopIntegrationTestConfiguration {

    @Bean
    public ShopIntegrationTestRunner shopIntegrationTestRunner() {
        return new ShopIntegrationTestRunner();
    }
}
