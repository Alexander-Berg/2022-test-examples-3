package ru.yandex.direct.manualtests.tasks.bsgoalsstat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.env.EnvironmentTypeProvider;
import ru.yandex.direct.grid.core.configuration.GridCoreConfiguration;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridCampaignYtRepository;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.core.util.yt.YtStatisticSenecaSasSupport;
import ru.yandex.direct.grid.core.util.yt.YtTestTablesSupport;
import ru.yandex.direct.manualtests.configuration.BaseConfiguration;
import ru.yandex.direct.ytcomponents.service.DirectGridStatDynContextProvider;

@Configuration
@ComponentScan(
        basePackages = "ru.yandex.direct.manualtests.tasks.bsgoalsstat",
        excludeFilters = {
                @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
        }
)
@Import({
        GridCoreConfiguration.class,
})
public class BsGoalsStatConfiguration extends BaseConfiguration {
    @Bean
    public GridCampaignYtRepository gridCampaignYtRepository(
            YtDynamicSupport gridYtSupport,
            YtStatisticSenecaSasSupport ytStatisticSenecaSasSupport,
            DirectGridStatDynContextProvider gridStatDynContextProvider,
            CampaignService campaignService,
            PpcPropertiesSupport ppcPropertiesSupport,
            YtTestTablesSupport ytTestTablesSupport,
            EnvironmentTypeProvider environmentTypeProvider) {
        return new GridCampaignYtRepository(
                gridYtSupport,
                ytStatisticSenecaSasSupport,
                gridStatDynContextProvider,
                ytTestTablesSupport,
                campaignService,
                ppcPropertiesSupport,
                environmentTypeProvider
        );
    }
}
