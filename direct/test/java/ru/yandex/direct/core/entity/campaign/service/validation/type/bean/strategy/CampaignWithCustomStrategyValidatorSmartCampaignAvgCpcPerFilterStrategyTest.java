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
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpcPerFilterStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидацию AUTOBUDGET_AVG_CPC_PER_FILTER стратегии SMART кампаний
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorSmartCampaignAvgCpcPerFilterStrategyTest {

    private static final BigDecimal FILER_AVG_BID = BigDecimal.valueOf(15.5);
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

    private SmartCampaign smartCampaign;
    private CampaignWithCustomStrategyValidator validator;
    private BigDecimal bidMin;
    private BigDecimal bidMax;
    private BigDecimal filterAvgBidMin;
    private BigDecimal filterAvgBidMax;
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
        filterAvgBidMin = currency.getMinCpcCpaPerformance();
        filterAvgBidMax = currency.getMaxAutobudgetBid();
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
    public void validateStrategy_WithoutFilterAvgBid_NotNull() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(null, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.FILTER_AVG_BID)), notNull())));
    }

    @Test
    public void validateStrategy_WithFilterAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(FILER_AVG_BID, BID, SUM));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_FilterAvgBidHigherThanBid_BidLessThanAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                new BigDecimal("501"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.FILTER_AVG_BID)), bidLessThanAvgBid())));
    }

    @Test
    public void validateStrategy_FilterAvgBidEqualToBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                new BigDecimal("500"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_FilterAvgBidEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                new BigDecimal("500"), null, new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_FilterAvgBidLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                new BigDecimal("500"), null, new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidEqualToSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, new BigDecimal("500"), new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLowerThanSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, new BigDecimal("500"), new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowFilterAvgBid_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                filterAvgBidMin.subtract(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.FILTER_AVG_BID)),
                greaterThanOrEqualTo(filterAvgBidMin))));
    }

    @Test
    public void validateStrategy_HighFilterAvgBid_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                filterAvgBidMax.add(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.FILTER_AVG_BID)),
                lessThanOrEqualTo(filterAvgBidMax))));
    }

    @Test
    public void validateStrategy_MinFilterAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(filterAvgBidMin, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxFilterAvgBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(filterAvgBidMax, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, null, sumMin.subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(sumMin))));
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, null, sumMax.add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(sumMax))));
    }

    @Test
    public void validateStrategy_MinSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(FILER_AVG_BID, null, sumMin));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(FILER_AVG_BID, null, sumMax));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, bidMin.subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(bidMin))));
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, bidMax.add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(bidMax))));
    }

    @Test
    public void validateStrategy_MaxBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(
                FILER_AVG_BID, bidMax, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MinBid() {
        smartCampaign.withStrategy(defaultAverageCpcPerFilterStrategy(filterAvgBidMin, bidMin, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(smartCampaign);
        if (bidMin.compareTo(filterAvgBidMin) < 0) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                    field(DbStrategy.STRATEGY_DATA), field(StrategyData.FILTER_AVG_BID)),
                    bidLessThanAvgBid())));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }
}
