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
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects;
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
import static ru.yandex.direct.core.testing.data.TestCampaigns.autoBudgetWeekBundle;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидность данных в стратегии 'оптимизация кликов (по пакету кликов)' у РМП
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignWeekBundleStrategyTest {

    private final long limitClicks = 300L;

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
                Collections.emptySet(),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), moblieCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, emptySet(), container, null);
    }

    @Test
    public void validateStrategy() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(100L, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Ниже проверка недельного количества кликов (LimitClicks)
     */

    @Test
    public void validateStrategy_WithoutLimitClicks_NotNull() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(null, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.LIMIT_CLICKS)), notNull())));
    }

    @Test
    public void validateStrategy_LowLimitClicks_LessThanMin() {
        moblieCampaign.withStrategy(
                autoBudgetWeekBundle(currency.getMinAutobudgetClicksBundle() - 1L, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.LIMIT_CLICKS)),
                greaterThanOrEqualTo((long) currency.getMinAutobudgetClicksBundle()))));
    }

    @Test
    public void validateStrategy_MinLimitClicks() {
        moblieCampaign.withStrategy(
                autoBudgetWeekBundle((long) currency.getMinAutobudgetClicksBundle(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxLimitClicks() {
        moblieCampaign.withStrategy(
                autoBudgetWeekBundle(currency.getMaxAutobudgetClicksBundle(), null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighLimitClicks_GreaterThanMax() {
        moblieCampaign.withStrategy(
                autoBudgetWeekBundle(currency.getMaxAutobudgetClicksBundle() + 1L, null, null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.LIMIT_CLICKS)),
                lessThanOrEqualTo(currency.getMaxAutobudgetClicksBundle()))));
    }

    /**
     * Ниже проверка максимальной цены клика (Bid)
     */

    @Test
    public void validateStrategy_LowBid_LessThanMin() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks,
                constants.getMinPrice().subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                greaterThanOrEqualTo(constants.getMinPrice()))));
    }

    @Test
    public void validateStrategy_MinBid() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, constants.getMinPrice(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxBid() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, currency.getMaxAutobudgetBid(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighBid_GreaterThanMax() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks,
                currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    /**
     * Ниже проверка средней цены клика за неделю (AvgBid)
     */

    @Test
    public void validateStrategy_LowAvgBid_LessThanMin() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, null,
                constants.getMinAvgPrice().subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                greaterThanOrEqualTo(constants.getMinAvgPrice()))));
    }

    @Test
    public void validateStrategy_MinAvgBid() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, null, constants.getMinAvgPrice()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgBid() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, null, currency.getMaxAutobudgetBid()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighAvgBid_GreaterThanMax() {
        moblieCampaign.withStrategy(autoBudgetWeekBundle(limitClicks, null,
                currency.getMaxAutobudgetBid().add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    /**
     * Проверка средней цены клика (AvgBid) с максимальной (Bid)
     */

    @Test
    public void validateStrategy_BidWithAvgBid_AvgBidAndBidTogetherProhibited() {
        moblieCampaign.withStrategy(
                autoBudgetWeekBundle(limitClicks, BigDecimal.valueOf(100), BigDecimal.valueOf(100)));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                StrategyDefects.avgBidAndBidTogetherAreProhibited())));
    }
}

