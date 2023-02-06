package ru.yandex.market.adv.yt.test.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.adv.yt.YtDynamicClientFactory;
import ru.yandex.market.adv.yt.YtStaticClientFactory;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Date: 14.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Configuration
@ParametersAreNonnullByDefault
public class TestConfiguration {

    @Bean
    public YtClientProxy ytStaticClient(YtStaticClientFactory ytStaticClientFactory) {
        return ytStaticClientFactory.createClient();
    }

    @Bean
    public YtClientProxy ytDynamicClient(YtDynamicClientFactory ytDynamicClientFactory) {
        return ytDynamicClientFactory.createClient();
    }
}
