package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.campaign.yql.AsyncYqlService;
import ru.yandex.market.crm.yql.SyncYqlService;
import ru.yandex.market.crm.yql.YqlTemplateService;
import ru.yandex.market.crm.yql.client.YqlClient;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
class MockYqlConfig {

    @Bean
    public SyncYqlService yqlService() {
        return mock(SyncYqlService.class);
    }

    @Bean
    public AsyncYqlService asyncYqlService() {
        return mock(AsyncYqlService.class);
    }

    @Bean
    public YqlClient yqlClient() {
        return mock(YqlClient.class);
    }

    @Bean
    public YqlTemplateService yqlTemplateService() {
        return mock(YqlTemplateService.class);
    }
}
