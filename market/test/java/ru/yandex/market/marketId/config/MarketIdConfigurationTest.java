package ru.yandex.market.marketId.config;

import java.util.Collection;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketIdConfigurationTest extends MarketIdConfiguration {

    //TODO: chage to random port
    public static final int TESTING_PORT = 41500;

    public MarketIdConfigurationTest(Collection<? extends io.grpc.BindableService> services) {
        super(TESTING_PORT, services);
    }
}
