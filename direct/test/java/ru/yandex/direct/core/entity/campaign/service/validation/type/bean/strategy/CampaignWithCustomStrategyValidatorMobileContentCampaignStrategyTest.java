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
import ru.yandex.direct.core.entity.retargeting.model.Goal;
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
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидность данных в стратегии 'оптимизация кликов (по недельному бюджету)' у РМП
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignStrategyTest {

    private final BigDecimal sum = BigDecimal.valueOf(1500L);
    private final long validGoalId = 38403191L;

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
    private CommonStrategyValidatorConstants constants;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        moblieCampaign = (MobileContentCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        constants = new CommonStrategyValidatorConstants(currency);

        CampaignValidationContainer container = CampaignValidationContainer
                .create(campaignInfo.getShard(), campaignInfo.getUid(), campaignInfo.getClientId());
        validator = new CampaignWithCustomStrategyValidator(currency,
                Set.of((Goal) new Goal().withId(validGoalId).withIsMobileGoal(true)),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), moblieCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, emptySet(), container, null);
    }

    @Test
    public void validateStrategy() {
        moblieCampaign.withStrategy(defaultAutobudgetStrategy());
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Ниже проверка недельного бюджета (Sum)
     */

    @Test
    public void validateStrategy_WithoutSum() {
        moblieCampaign.withStrategy(autobudgetStrategy(null, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)), notNull())));
    }

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        moblieCampaign.withStrategy(
                autobudgetStrategy(currency.getMinAutobudget().subtract(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(currency.getMinAutobudget()))));
    }

    @Test
    public void validateStrategy_MinSum() {
        moblieCampaign.withStrategy(autobudgetStrategy(currency.getMinAutobudget(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        moblieCampaign.withStrategy(autobudgetStrategy(currency.getMaxAutobudget(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        moblieCampaign.withStrategy(
                autobudgetStrategy(currency.getMaxAutobudget().add(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }

    /**
     * Ниже проверка максимальной цены клика (Bid)
     */

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        moblieCampaign.withStrategy(
                autobudgetStrategy(sum, constants.getMinPrice().subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(constants.getMinPrice()))));
    }

    @Test
    public void validateStrategy_MinBid() {
        moblieCampaign.withStrategy(autobudgetStrategy(sum, constants.getMinPrice(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxBid() {
        moblieCampaign.withStrategy(autobudgetStrategy(currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")),
                currency.getMaxAutobudgetBid(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        moblieCampaign.withStrategy(
                autobudgetStrategy(sum, currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    /**
     * Ниже проверка недельного бюджета (Sum) с максимальной ценой клика (Bid)
     */

    @Test
    public void validateStrategy_BidEqualToSum() {
        moblieCampaign.withStrategy(autobudgetStrategy(new BigDecimal("500"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLowerThanSum() {
        moblieCampaign.withStrategy(autobudgetStrategy(new BigDecimal("500"), new BigDecimal("500.01"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)), weekBudgetLessThan())));
    }


    /**
     * Ниже проверка целей
     */

    @Test
    public void validateStrategy_WithUnsupportedGoalId() {
        long notExistingId = 1L;
        moblieCampaign.withStrategy(autobudgetStrategy(sum, null, notExistingId));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), objectNotFound())));
    }

    @Test
    public void validateStrategy_WithAllGoals_MustBeValidId() {
        long notValidId = 0L;
        moblieCampaign.withStrategy(autobudgetStrategy(sum, null, notValidId));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), validId())));
    }

    @Test
    public void validateStrategy_WithValidGoalId() {
        moblieCampaign.withStrategy(autobudgetStrategy(sum, null, validGoalId));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
