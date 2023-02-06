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
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
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
public class CampaignWithCustomStrategyValidatorMcBannerCampaignAvgCpcPerCampStrategyTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Currency currency;
    private McBannerCampaign mcBannerCampaign;
    private CampaignWithCustomStrategyValidator validator;
    private CommonStrategyValidatorConstants constants;

    @Before
    public void before() {
        var mcBannerCampaign = steps.mcBannerCampaignSteps().createDefaultCampaign();
        currency = clientService.getWorkCurrency(mcBannerCampaign.getClientId());
        this.mcBannerCampaign = (McBannerCampaign) campaignTypedRepository
                .getTypedCampaigns(mcBannerCampaign.getShard(), singletonList(mcBannerCampaign.getId())).get(0);
        constants = new CommonStrategyValidatorConstants(currency);
        CampaignValidationContainer container = CampaignValidationContainer
                .create(mcBannerCampaign.getShard(), mcBannerCampaign.getUid(), mcBannerCampaign.getClientId());

        validator = new CampaignWithCustomStrategyValidator(currency,
                Collections.emptySet(),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), this.mcBannerCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, Collections.emptySet(), container, null);
    }

    @Test
    public void validateStrategy_WithoutAvgBid_NotNull() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                null, new BigDecimal("100"), new BigDecimal("5000")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)), notNull())));
    }

    @Test
    public void validateStrategy_WithAvgBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("100"), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("5000")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_AvgBidHigherThanBid_BidLessThanAvgBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("501"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)), bidLessThanAvgBid())));
    }

    @Test
    public void validateStrategy_AvgBidEqualToBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), new BigDecimal("500"), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_AvgBidEqualToSum() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), null, new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_AvgBidLowerThanSum() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("500"), null, new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidEqualToSum_WeekBudgetLessThan() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_BidLowerThanSum() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("501")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_LowAvgBid_LessThanMin() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                constants.getMinAvgPrice().subtract(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                greaterThanOrEqualTo(constants.getMinAvgPrice()))));
    }

    @Test
    public void validateStrategy_HighAvgBid_GreaterThanMax() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    @Test
    public void validateStrategy_MinAvgBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                constants.getMinAvgPrice(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                currency.getMaxAutobudgetBid(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(constants.getMinAvgPrice(),
                null, currency.getMinAutobudget().subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(currency.getMinAutobudget()))));
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(constants.getMinAvgPrice(),
                null, currency.getMaxAutobudget().add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }

    @Test
    public void validateStrategy_MinSum() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                constants.getMinAvgPrice(), null, currency.getMinAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                constants.getMinAvgPrice(), null, currency.getMaxAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(constants.getMinAvgPrice(),
                currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(constants.getMinAvgPrice(),
                constants.getMinPrice().subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(constants.getMinPrice()))));
    }

    @Test
    public void validateStrategy_MaxBid() {
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(
                constants.getMinAvgPrice(), currency.getMaxAutobudgetBid(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MinBid() {
        BigDecimal lowBid = constants.getMinPrice();
        BigDecimal lowAvgBid = constants.getMinAvgPrice();
        mcBannerCampaign.withStrategy(defaultAverageCpcPerCamprStrategy(lowAvgBid, lowBid, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mcBannerCampaign);
        if (lowBid.compareTo(lowAvgBid) < 0) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                    field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                    bidLessThanAvgBid())));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }
}
