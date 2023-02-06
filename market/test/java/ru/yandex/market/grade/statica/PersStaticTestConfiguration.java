package ru.yandex.market.grade.statica;

import java.io.ByteArrayInputStream;

import org.apache.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.grade.statica.config.CoreConfig;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@Configuration
// all configs from config package (filter with profile)
// and all basket service/components
@ComponentScan(basePackageClasses = CoreConfig.class)
@ComponentScan(
    basePackageClasses = PersStaticMain.class,
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
public class PersStaticTestConfiguration {
    @Bean
    public HttpClient saasHttpClient() {
        return PersTestMocksHolder.registerMock(HttpClient.class, client ->
            HttpClientMockUtils.mockResponse(client, req -> new ByteArrayInputStream(new byte[0])));
    }

}
