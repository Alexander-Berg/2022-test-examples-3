package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.campaign.test.utils.IntTestUtilsConfig;
import ru.yandex.market.crm.campaign.test.yt.CampaignLocalYtConfig;

/**
 * @author apershukov
 */
@Configuration
@Import({
        CoreServiceIntTestConfig.class,
        LocalYqlConfig.class,
        CampaignLocalYtConfig.class,
        IntTestUtilsConfig.class
})
class ServiceLargeTestConfig {
}
