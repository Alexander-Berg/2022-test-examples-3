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
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.PERFORMANCE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MAX;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MAX;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ROI_COEF_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.incorrectReserveReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetRoiStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorSmartCampaignRoiStrategyTest {
    private static final long VALID_GOAL_ID = 0L;
    private static final BigDecimal SUM = BigDecimal.valueOf(5005.5);
    private static final BigDecimal BID = BigDecimal.valueOf(500.5);
    private static final Long RESERVE_RETURN = 10L;

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private SmartCampaign smartCampaign;
    private CampaignWithCustomStrategyValidator validator;
    private BigDecimal bidMin;
    private BigDecimal bidMax;
    private BigDecimal sumMin;
    private BigDecimal sumMax;

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

        bidMin = currency.getMinCpcCpaPerformance();
        bidMax = currency.getMaxAutobudgetBid();
        sumMin = currency.getMinAutobudget();
        sumMax = currency.getMaxAutobudget();

        validator = new CampaignWithCustomStrategyValidator(currency,
                Collections.emptySet(),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), smartCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, Collections.emptySet(), container, null);
    }

    @Test
    public void validateStrategy_WithoutRoiCoef_NotNull() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(
                null, null, null, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.ROI_COEF)), notNull())));
    }

    @Test
    public void validateStrategy_WithoutReserveReturn_NotNull() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(
                null, null, BigDecimal.ZERO, null, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.RESERVE_RETURN)), notNull())));
    }

    @Test
    public void validateStrategy() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(
                SUM, BID, BigDecimal.ZERO, RESERVE_RETURN, new BigDecimal("10"), VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowRoiCoef_LessThanMin() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(
                null, null, ROI_COEF_MIN, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.ROI_COEF)),
                greaterThan(ROI_COEF_MIN))));
    }

    @Test
    public void validateStrategy_MinRoiCoef() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                ROI_COEF_MIN.add(new BigDecimal("0.1")), RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_NotExistGoal_ObjectNotFound() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(
                null, null, ROI_COEF_MIN, RESERVE_RETURN, null, Long.MAX_VALUE));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)),
                objectNotFound())));
    }

    @Test
    public void validateStrategy_LowReserveReturn_IncorrectReserveReturn() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN_MIN - 10L, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.RESERVE_RETURN)),
                incorrectReserveReturn())));
    }

    @Test
    public void validateStrategy_MinReserveReturn() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN_MIN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_HighReserveReturn_IncorrectReserveReturn() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN_MAX + 10L, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.RESERVE_RETURN)),
                incorrectReserveReturn())));
    }

    @Test
    public void validateStrategy_MaxReserveReturn() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN_MAX, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_IncorrectStepReserveReturn_IncorrectReserveReturn() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, 55L, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.RESERVE_RETURN)),
                incorrectReserveReturn())));
    }

    @Test
    public void validateStrategy_LowProfitability_LessThanMin() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN, PROFITABILITY_MIN.subtract(new BigDecimal("0.1")), VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.PROFITABILITY)),
                greaterThanOrEqualTo(PROFITABILITY_MIN))));
    }

    @Test
    public void validateStrategy_MinProfitability() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN, PROFITABILITY_MIN, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighProfitability_GreaterThanMax() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN, PROFITABILITY_MAX.add(new BigDecimal("0.1")), VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.PROFITABILITY)),
                lessThanOrEqualTo(PROFITABILITY_MAX))));
    }

    @Test
    public void validateStrategy_MaxProfitability() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, null,
                BigDecimal.ZERO, RESERVE_RETURN, PROFITABILITY_MAX, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(sumMin.subtract(new BigDecimal("0.1")), null,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(sumMin))));
    }

    @Test
    public void validateStrategy_MinSum() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(sumMin, null,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(sumMax.add(new BigDecimal("0.1")), null,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(sumMax))));
    }

    @Test
    public void validateStrategy_MaxSum() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(sumMax, null,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, bidMin.subtract(new BigDecimal("0.1")),
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(bidMin))));
    }

    @Test
    public void validateStrategy_MinBid() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, bidMin,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, bidMax.add(new BigDecimal("0.1")),
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(bidMax))));
    }

    @Test
    public void validateStrategy_MaxBid() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(null, bidMax,
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidEqualToSum() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(new BigDecimal("500"), new BigDecimal("500"),
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLessThanSum() {
        smartCampaign.withStrategy(autobudgetRoiStrategy(new BigDecimal("501"), new BigDecimal("500"),
                BigDecimal.ZERO, RESERVE_RETURN, null, VALID_GOAL_ID));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
