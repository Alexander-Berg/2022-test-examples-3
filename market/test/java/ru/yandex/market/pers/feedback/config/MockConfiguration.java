package ru.yandex.market.pers.feedback.config;

import java.time.Clock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.08.2021
 */
@Configuration
public class MockConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer checkouterMock() {
        return new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    }

    @Bean
    public TvmClient tvmClient() {
        return PersTestMocksHolder.registerMock(TvmClient.class, tvmClient -> {
            when(tvmClient.getServiceTicketFor(anyInt())).thenReturn("aServiceTicket");
        });
    }

    @Bean
    public PersAuthorClient persAuthorClient() {
        return PersTestMocksHolder.registerMock(PersAuthorClient.class);
    }

    @Bean
    public Clock testableClock() {
        return new TestableClock();
    }
}
