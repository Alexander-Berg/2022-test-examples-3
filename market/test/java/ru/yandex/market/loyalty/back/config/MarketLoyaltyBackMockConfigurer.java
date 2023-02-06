package ru.yandex.market.loyalty.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.loyalty.back.security.BackClientSupplier;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.loyalty.core.mock.MarketLoyaltyCoreMockConfigurer.MOCKS;

@Configuration
public class MarketLoyaltyBackMockConfigurer {
    @Configuration
    public static class TvmConfig {
        @Bean
        public BackClientSupplier backClientSupplier() {
            final BackClientSupplier mock = mock(BackClientSupplier.class);
            MOCKS.put(mock, null);
            return mock;
        }
    }
}
