package ru.yandex.market.checker.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerInterceptor;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checker.controller.interceptor.MockAuthInterceptor;
import ru.yandex.market.checker.service.MockSecManager;
import ru.yandex.market.checker.st.StService;
import ru.yandex.market.checker.st.client.STClient;
import ru.yandex.market.checker.yql.CheckerYqlClient;
import ru.yandex.market.checker.yql.client.YqlClient;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.security.SecManager;

@Configuration
@PropertySource({"classpath:startrek-test.properties"})
public class FunctionalTestConfig {
    private static final LocalDateTime TEST_UPDATE_TIME = LocalDateTime.of(2020, 10, 1, 10, 0, 0);

    @Value("${st.url:}")
    private String stUrl;

    @Value("${st.checker.queue}")
    private String checkerQueue;

    @Value("${st.check.summary.template:}")
    private String stCheckSummaryTemplate;

    @Value("${st.check.description.template:}")
    private String stCheckDescriptionTemplate;

    @Value("${st.import.summary.template:}")
    private String stImportSummaryTemplate;

    @Value("${st.import.description.template:}")
    private String stImportDescriptionTemplate;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        final var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public Clock clock() {
        return Clock.fixed(DateTimes.toInstantAtDefaultTz(TEST_UPDATE_TIME), ZoneId.systemDefault());
    }

    @Bean
    public HandlerInterceptor authInterceptor() {
        return new MockAuthInterceptor();
    }

    @Bean
    public YqlClient yqlClient(WebClient webClient) {
        return Mockito.mock(CheckerYqlClient.class);
    }

    @Bean
    public STClient stClient() {
        return Mockito.mock(STClient.class);
    }

    @Bean
    public StService stService(STClient stClient) {
        return new StService(
                checkerQueue,
                stCheckSummaryTemplate,
                stCheckDescriptionTemplate,
                stImportSummaryTemplate,
                stImportDescriptionTemplate,
                stClient,
                stUrl);
    }

    @Bean
    public Tvm2 checkerTvm() {
        return Mockito.mock(Tvm2.class);
    }

    @Bean
    public SecManager secManager() {
        return new MockSecManager();
    }
}
