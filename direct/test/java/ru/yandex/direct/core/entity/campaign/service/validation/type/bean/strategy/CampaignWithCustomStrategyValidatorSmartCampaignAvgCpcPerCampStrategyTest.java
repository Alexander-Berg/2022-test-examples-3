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
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.bidLessThanAvgBid;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpcPerCamprStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorSmartCampaignAvgCpcPerCampStrategyTest {

    private static final BigDecimal AVG_BID = BigDecimal.valueOf(15.5);
    private static final BigDecimal SUM = BigDecimal.valueOf(5005.5);
    private static final BigDecimal BID = BigDecimal.valueOf(500.5);

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
    private BigDecimal avgBidMin;
    private BigDecimal avgBidMax;
    private BigDecimal sumMin;
    private BigDecimal sumMax;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        smartCampaign = (SmartCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        StrategyValidatorConstants constants = StrategyValidatorConstantsBuilder.build(PERFORMANCE, currency);
        CampaignValidationContainer container = CampaignValidationContainer.create(campaignInfo.getShard(),
                campaignInfo.getUid(),
                campaignInfo.getClientId());

        bidMin = currency.getMinCpcCpaPerformance();
        bidMax = currency.getMaxAutobudgetBid();
        avgBidMin = currency.getMinCpcCpaPerformance();
        avgBidMax = currency.getMaxAutobudgetBid();
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
    public void validateStrategy_WithoutAvgBid_NotNull() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(null, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)), notNull())));
    }

    @Test
    public void validateStrategy_WithAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(AVG_BID, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(AVG_BID, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_AvgBidHigherThanBid_BidLessThanAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("501"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)), bidLessThanAvgBid())));
    }

    @Test
    public void validateStrategy_AvgBidEqualToBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_AvgBidEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), null, new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_AvgBidLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), null, new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, new BigDecimal("500"), new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, new BigDecimal("500"), new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowAvgBid_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                avgBidMin.subtract(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                greaterThanOrEqualTo(avgBidMin))));
    }

    @Test
    public void validateStrategy_HighAvgBid_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                avgBidMax.add(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    @Test
    public void validateStrategy_MinAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(avgBidMin, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(avgBidMax, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, null, sumMin.subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(sumMin))));
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, null, sumMax.add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }

    @Test
    public void validateStrategy_MinSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(AVG_BID, null, sumMin));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(AVG_BID, null, sumMax));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, bidMax.add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                AVG_BID, bidMin.subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(bidMin))));
    }

    @Test
    public void validateStrategy_MaxBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(AVG_BID, bidMax, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MinBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(avgBidMin, bidMin, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        if (bidMin.compareTo(avgBidMin) < 0) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                    field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                    bidLessThanAvgBid())));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }
}
