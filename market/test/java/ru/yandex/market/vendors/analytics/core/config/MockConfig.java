package ru.yandex.market.vendors.analytics.core.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.mds.MdsJsonReader;
import ru.yandex.market.vendors.analytics.core.security.AnalyticsTvmClient;
import ru.yandex.market.vendors.analytics.core.service.startrek.StartrekClient;

import static org.mockito.Mockito.mock;

@Configuration
public class MockConfig {

    @Bean
    public ShopSalesDAO shopSalesDAO() {
        return mock(ShopSalesDAO.class);
    }

    @Bean
    public StartrekClient startrekClient() {
        return Mockito.mock(StartrekClient.class);
    }

    @Bean
    public MdsJsonReader mdsJsonReader() {
        return mock(MdsJsonReader.class);
    }

    @Bean
    public AnalyticsTvmClient analyticsTvmClient() {
        return mock(AnalyticsTvmClient.class);
    }

    @Bean
    public Yt yt(){
        return mock(Yt.class);
    }
}
