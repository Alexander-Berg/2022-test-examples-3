package ru.yandex.market.supportwizard.config;


import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.supportwizard.importing.YTAgenciesDataDAO;
import ru.yandex.market.supportwizard.importing.YtPartnerDataDAO;
import ru.yandex.market.supportwizard.service.StartrekSessionBuilder;
import ru.yandex.market.supportwizard.service.build.BuildLogEntryConsumer;
import ru.yandex.market.supportwizard.yqlapi.YqlApiService;
import ru.yandex.startrek.client.Session;

@Configuration
public class MockConfig {
    @MockBean
    private Session startrekSession;

    @MockBean
    private YtPartnerDataDAO ytPartnerDataDAO;

    @MockBean
    private YTAgenciesDataDAO ytAgenciesDataDAO;

    @MockBean
    private Terminal terminal;

    @MockBean
    private StartrekSessionBuilder startrekSessionBuilder;

    @MockBean
    private MbiApiClient mbiApiClient;

    @MockBean
    private BuildLogEntryConsumer buildLogEntryConsumer;

    @MockBean
    private YqlApiService yqlApiService;

    @Bean
    public PropertySourcesPlaceholderConfigurer configurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }
}
