package ru.yandex.market.adv.b2bmonetization.config;

import java.util.Objects;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import ru.yandex.market.adv.b2bmonetization.properties.yt.YtPricelabsTableProperties;

/**
 * Конфиг для тестов с дополнительными тестовыми классами.
 * Date: 21.04.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@Configuration
@ParametersAreNonnullByDefault
@EnableConfigurationProperties(YtPricelabsTableProperties.class)
public class TestConfig {

    @Autowired
    private YtPricelabsTableProperties ytPricelabsTableProperties;

    @Nonnull
    @Bean
    public BiConsumer<String, Runnable> ytRunner() {
        return (newPrefix, runnable) -> {
            String oldPrefix = ytPricelabsTableProperties.getPrefix();
            ytPricelabsTableProperties.setPrefix("//tmp/" + newPrefix);
            try {
                runnable.run();
                ytPricelabsTableProperties.setPrefix(oldPrefix);
            } catch (Throwable e) {
                ytPricelabsTableProperties.setPrefix(oldPrefix);
                throw e;
            }
        };
    }

    @Bean("offerRecommendationResource")
    public Resource offerRecommendationResource() {
        return new UrlResource(
                Objects.requireNonNull(
                        getClass().getResource("template/marketplace-recommended-prices-promocodes-testing.xlsm")
                )
        );
    }
}
