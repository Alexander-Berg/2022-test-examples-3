package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.PERFORMANCE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.feature.FeatureName.AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидность данных в стратегии 'оптимизация конверсий (по недельному бюджету)' у смарт-кампаний
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorSmartCampaignStrategyTest {
    private static final BigDecimal SUM = BigDecimal.valueOf(5005.5);
    private static final BigDecimal BID = BigDecimal.valueOf(500.5);
    private static final Long GOAL_ID = CampaignStrategyTestDataUtils.CAMPAIGN_COUNTER_GOAL_1;

    private final Set<String> availableFeatures = ImmutableSet.of(AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED.getName());

    @Autowired
    private ClientService clientService;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private SmartCampaign smartCampaign;
    private CampaignWithCustomStrategyValidator validator;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        Currency currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        smartCampaign = (SmartCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        StrategyValidatorConstants constants = StrategyValidatorConstantsBuilder.build(PERFORMANCE, currency);
        CampaignValidationContainer container = CampaignValidationContainer.create(campaignInfo.getShard(),
                campaignInfo.getUid(),
                campaignInfo.getClientId());

        validator = new CampaignWithCustomStrategyValidator(currency,
                CampaignStrategyTestDataUtils.CAMPAIGN_COUNTERS_AVAILABLE_GOALS,
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), smartCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, availableFeatures, container, null);
    }

    @Test
    public void validateStrategy() {
        smartCampaign.withStrategy(autobudgetStrategy(SUM, BID, GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_NoBid() {
        smartCampaign.withStrategy(autobudgetStrategy(SUM, null, GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_NoGoal() {
        smartCampaign.withStrategy(autobudgetStrategy(SUM, BID, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), notNull())));
    }
}
