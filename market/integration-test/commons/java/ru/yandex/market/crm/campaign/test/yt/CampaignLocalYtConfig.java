package ru.yandex.market.crm.campaign.test.yt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.chyt.services.ChytQueryExecutor;
import ru.yandex.market.crm.core.test.LocalYtConfig;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
@Import(LocalYtConfig.class)
public class CampaignLocalYtConfig {

    @Bean
    public ChytQueryExecutor chytQueryExecutor() {
        return mock(ChytQueryExecutor.class);
    }
}
