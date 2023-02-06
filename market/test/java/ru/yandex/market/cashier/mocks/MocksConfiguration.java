package ru.yandex.market.cashier.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.cashier.mocks.trust.BasketResponseTransformer;
import ru.yandex.market.cashier.mocks.wiremock.DynamicWiremockFactoryBean;
import ru.yandex.market.cashier.mocks.wiremock.RandomInjectingResponseTransformer;

@Configuration
public class MocksConfiguration {
    @Bean(initMethod = "start",destroyMethod = "stop")
    public WireMockServer trustMock(){
        return DynamicWiremockFactoryBean.create(
                new BasketResponseTransformer(),
                new RandomInjectingResponseTransformer());
    }

    @Bean(initMethod = "start",destroyMethod = "stop")
    public WireMockServer sberMock(){
        return DynamicWiremockFactoryBean.create();
    }
}
