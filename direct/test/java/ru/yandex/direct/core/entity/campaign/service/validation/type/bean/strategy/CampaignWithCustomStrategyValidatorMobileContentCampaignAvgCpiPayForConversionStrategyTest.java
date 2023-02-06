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
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
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
 * Тесты на валидность данных в стратегии 'Оптимизация конверсий c оплатой за конверсии' у РМП
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignAvgCpiPayForConversionStrategyTest {

    private final long validGoalId = 38403191L;
    private final BigDecimal avgCpi = new BigDecimal("100");

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private CampaignValidationContainer container;
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

        container = CampaignValidationContainer
                .create(campaignInfo.getShard(), campaignInfo.getUid(), campaignInfo.getClientId());
        validator = initValidator();
    }

    @Test
    public void validateStrategy_WithPayForConversion() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Ниже проверка средней цены конверсии за неделю (AvgCpi)
     */

    @Test
    public void validateStrategy_WithoutAvgCpi_NotNull() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, null, null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPI)), notNull())));
    }

    @Test
    public void validateStrategy_LowAvgCpi_LessThanMin() {
        moblieCampaign.withStrategy(averageCpiStrategy(
                validGoalId, constants.getMinAvgCpa().subtract(new BigDecimal("0.1")), null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPI)),
                greaterThanOrEqualTo(constants.getMinAvgCpa()))));
    }

    @Test
    public void validateStrategy_MinAvgCpi() {
        moblieCampaign.withStrategy(averageCpiStrategy(
                validGoalId, constants.getMinAvgCpa(), null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgCpi() {
        moblieCampaign.withStrategy(averageCpiStrategy(
                validGoalId, currency.getAutobudgetPayForConversionAvgCpaWarning(), null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighAvgCpi_GreaterThanMax() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId,
                currency.getAutobudgetPayForConversionAvgCpaWarning().add(new BigDecimal("0.1")), null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPI)),
                lessThanOrEqualTo(currency.getAutobudgetPayForConversionAvgCpaWarning()))));
    }

    /**
     * Ниже проверка недельного бюджета (Sum)
     */

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, null,
                currency.getMinAutobudget().subtract(new BigDecimal("0.1")), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(currency.getMinAutobudget()))));
    }

    @Test
    public void validateStrategy_LowSumToAvgCpi_LessThanMin() {
        BigDecimal minSumLimit = avgCpi.multiply(BigDecimal.valueOf(PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO));

        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, null,
                minSumLimit.subtract(BigDecimal.valueOf(0.01)), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MinSum() {
        BigDecimal minSumLimit = avgCpi.multiply(BigDecimal.valueOf(PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO));

        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, null, minSumLimit, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        moblieCampaign.withStrategy(
                averageCpiStrategy(validGoalId, avgCpi, null, currency.getMaxAutobudget(), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, null,
                currency.getMaxAutobudget().add(new BigDecimal("0.1")), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }

    /**
     * Проверка максимальной цены клика (Bid)
     */

    @Test
    public void validateStrategy_WithBid_MustBeNull() {
        moblieCampaign.withStrategy(averageCpiStrategy(validGoalId, avgCpi, BigDecimal.valueOf(100), null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)), CommonDefects.isNull())));
    }

    /**
     * Ниже проверка целей
     */

    @Test
    public void validateStrategy_WithUnsupportedGoalId() {
        moblieCampaign.withStrategy(averageCpiStrategy(
                1L, avgCpi, new BigDecimal("100"), new BigDecimal("300"), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), objectNotFound())));
    }

    @Test
    public void validateStrategy_WithAllGoals_MustBeValidId() {
        moblieCampaign.withStrategy(averageCpiStrategy(
                0L, avgCpi, new BigDecimal("100"), new BigDecimal("300"), true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), validId())));
    }

    @Test
    public void validateStrategy_WithoutGoalId() {
        moblieCampaign.withStrategy(averageCpiStrategy(null, avgCpi, null, null, true));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    private CampaignWithCustomStrategyValidator initValidator() {
        return new CampaignWithCustomStrategyValidator(currency, Set.of((Goal) new Goal().withId(validGoalId).withIsMobileGoal(true)),
                Collections::emptyList, Collections::emptyList,
                banners -> emptyList(), moblieCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, ImmutableSet.of(), container, null);
    }
}
