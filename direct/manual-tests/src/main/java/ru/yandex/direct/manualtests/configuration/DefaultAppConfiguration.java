package ru.yandex.direct.manualtests.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.container.StubAffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;

import static ru.yandex.direct.core.configuration.CoreConfiguration.ACCESSIBLE_CAMPAIGN_CHEKER_PROVIDER;

@Configuration
@ComponentScan(
        basePackages = "ru.yandex.direct.manualtests",
        excludeFilters = {
                @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
        }
)
public class DefaultAppConfiguration extends BaseConfiguration{
    @Bean
    public AffectedCampaignIdsContainer affectedCampaignIdsContainer() {
        return new StubAffectedCampaignIdsContainer();
    }

    @Bean(name = ACCESSIBLE_CAMPAIGN_CHEKER_PROVIDER)
    public RequestCampaignAccessibilityCheckerProvider accessibilityCheckerProvider() {
        return new RequestCampaignAccessibilityCheckerProvider();
    }
}
