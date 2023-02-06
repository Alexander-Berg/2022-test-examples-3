package ru.yandex.market.checkout.checkouterpumpkin.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        SpringApplicationConfig.class,
})
public class SpringApplicationTestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer checkouterMock() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
        );
    }
}
