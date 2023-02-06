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
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.PERFORMANCE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaPerCamprStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorSmartCampaignAvgCpaPerCampStrategyTest {
    private static final long VALID_GOAL_ID = 0L;
    private static final BigDecimal AVG_CPA = BigDecimal.valueOf(15.5);
    private static final BigDecimal BID = BigDecimal.valueOf(500.5);
    private static final BigDecimal SUM = BigDecimal.valueOf(5005.5);

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Currency currency;
    private SmartCampaign smartCampaign;
    private CampaignWithCustomStrategyValidator validator;
    private BigDecimal bidMin;
    private BigDecimal bidMax;
    private BigDecimal avgCpaMin;
    private BigDecimal avgCpaMax;
    private BigDecimal sumMin;
    private BigDecimal sumMax;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        smartCampaign = (SmartCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        StrategyValidatorConstants constants = StrategyValidatorConstantsBuilder.build(PERFORMANCE, currency);

        bidMin = currency.getMinCpcCpaPerformance();
        bidMax = currency.getMaxAutobudgetBid();
        avgCpaMin = currency.getMinCpcCpaPerformance();
        avgCpaMax = currency.getAutobudgetAvgCpaWarning();
        sumMin = currency.getMinAutobudget();
        sumMax = currency.getMaxAutobudget();

        CampaignValidationContainer container = CampaignValidationContainer.create(campaignInfo.getShard(),
                campaignInfo.getUid(),
                campaignInfo.getClientId());
        validator = new CampaignWithCustomStrategyValidator(currency,
                Collections.emptySet(),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), smartCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, ImmutableSet.of(), container, null);
    }

    @Test
    public void validateStrategy_WithoutAvgCpa_NotNull() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, null, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPA)), notNull())));
    }

    @Test
    public void validateStrategy_WithAvgCpa() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, AVG_CPA, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, AVG_CPA, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_AvgCpaIsEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, new BigDecimal("500"), null, new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_AvgCpaLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, new BigDecimal("500"), null, new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, new BigDecimal("500"), new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, new BigDecimal("500"), new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowAvgCpa_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, avgCpaMin.subtract(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPA)),
                greaterThanOrEqualTo(avgCpaMin))));
    }

    @Test
    public void validateStrategy_HighAvgCpa_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, avgCpaMax.add(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_CPA)),
                lessThanOrEqualTo(avgCpaMax))));
    }

    @Test
    public void validateStrategy_MinAvgCpa() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, avgCpaMin, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgCpa() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, avgCpaMax, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, currency.getMinCpcCpaPerformance(), null, sumMin.subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(sumMin))));
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, currency.getMinCpcCpaPerformance(), null, sumMax.add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(sumMax))));
    }

    @Test
    public void validateStrategy_MinSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, null, sumMin));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, null, sumMax));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, bidMax.add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(bidMax))));
    }

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(
                VALID_GOAL_ID, AVG_CPA, bidMin.subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(bidMin))));
    }

    @Test
    public void validateStrategy_MaxBid() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, AVG_CPA, bidMax, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MinBid() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(VALID_GOAL_ID, AVG_CPA, bidMin, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_NotExistGoal_ObjectNotFound() {
        smartCampaign.withStrategy(defaultAverageCpaPerCamprStrategy(Long.MAX_VALUE, AVG_CPA, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)),
                objectNotFound())));
    }
}
