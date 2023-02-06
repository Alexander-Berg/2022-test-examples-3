package ru.yandex.market.ydb.integration.context.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.trace.Tracer;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.ydb.integration.application.ProductionReadyConfig;
import ru.yandex.market.ydb.integration.application.BaseYdbConfig;

@Configuration
@Import({
        ProductionReadyConfig.class,
        BaseYdbConfig.class
})
@EnableConfigurationProperties({
        TestYdbProperties.class
})
@PropertySource("classpath:ydb-test.properties")
public class TestYdbConfig {

    @Bean
    public Tracer tracer() {
        return new Tracer(Module.MARKET_CIFACE_PROMO);
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }
}
