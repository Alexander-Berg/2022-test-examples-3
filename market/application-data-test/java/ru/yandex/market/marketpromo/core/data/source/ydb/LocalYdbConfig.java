package ru.yandex.market.marketpromo.core.data.source.ydb;

import com.yandex.ydb.table.TableClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.common.trace.Tracer;
import ru.yandex.market.marketpromo.core.application.properties.YdbProperties;
import ru.yandex.market.marketpromo.core.config.ydb.YdbConfig;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.ydb.integration.initialization.TableClientFactory;

@Configuration
@Import({
        YdbConfig.class
})
@EnableConfigurationProperties({
        YdbProperties.class
})
public class LocalYdbConfig {

    @Bean
    public Tracer tracer() {
        return new Tracer(Module.MARKET_CIFACE_PROMO);
    }

    @Bean
    public TableClient tableClient(TableClientFactory tableClientFactory) {
        return tableClientFactory.client();
    }

}
