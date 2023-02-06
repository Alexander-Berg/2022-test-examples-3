package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
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
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
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
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидность данных в стратегии 'Ручное управление ставками с оптимизацией' у РМП
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignDefaultStrategyTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Currency currency;
    private MobileContentCampaign moblieCampaign;
    private CampaignWithCustomStrategyValidator validator;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
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
    public void validateStrategy_WithoutSum() {
        moblieCampaign.withStrategy(manualBothStrategy());
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        moblieCampaign.withStrategy(manualBothStrategy(currency.getMinAutobudget().subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(currency.getMinAutobudget()))));
    }

    @Test
    public void validateStrategy_MinSum() {
        moblieCampaign.withStrategy(manualBothStrategy(currency.getMinAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        moblieCampaign.withStrategy(manualBothStrategy(currency.getMaxAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        moblieCampaign.withStrategy(manualBothStrategy(currency.getMaxAutobudget().add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }
}
