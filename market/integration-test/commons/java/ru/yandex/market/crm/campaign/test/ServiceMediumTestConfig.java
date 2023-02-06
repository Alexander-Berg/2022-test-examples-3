package ru.yandex.market.crm.campaign.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.campaign.test.yt.CampaignLocalYtConfig;
import ru.yandex.market.crm.external.blackbox.YandexTeamBlackboxClient;


/**
 * @author apershukov
 */
@Configuration
@Import({
        CoreServiceIntTestConfig.class,
        MockYqlConfig.class,
        CampaignLocalYtConfig.class
})
class ServiceMediumTestConfig {
    @Bean
    public YandexTeamBlackboxClient yandexTeamBlackboxClient() {
        return Mockito.mock(YandexTeamBlackboxClient.class);
    }
}
