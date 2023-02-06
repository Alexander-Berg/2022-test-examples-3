package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualBothStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

/**
 * Тесты на валидность данных в стратегии 'Ручное управление ставками с оптимизацией' у MCBANNER/ГО
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMcBannerCampaignDefaultStrategyTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private MobileContentCampaign moblieCampaign;
    private CampaignWithCustomStrategyValidator validator;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        Currency currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        moblieCampaign = (MobileContentCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        CommonStrategyValidatorConstants constants = new CommonStrategyValidatorConstants(currency);

        CampaignValidationContainer container = CampaignValidationContainer
                .create(campaignInfo.getShard(), campaignInfo.getUid(), campaignInfo.getClientId());
        validator = new CampaignWithCustomStrategyValidator(currency,
                Collections.emptySet(),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), moblieCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, emptySet(), container, null);
    }

    @Test
    public void validateStrategy() {
        moblieCampaign.withStrategy(manualBothStrategy());
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
