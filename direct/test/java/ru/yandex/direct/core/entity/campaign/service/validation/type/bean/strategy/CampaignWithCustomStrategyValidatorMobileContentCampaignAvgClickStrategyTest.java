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
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageClickStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на валидность данных в стратегии 'оптимизация кликов (по средней цене клика)' у РМП
 */
@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignAvgClickStrategyTest {

    private final BigDecimal avgBid = new BigDecimal("100");

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
        moblieCampaign.withStrategy(defaultAverageClickStrategy());
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Ниже проверка недельного бюджета (Sum)
     */

    @Test
    public void validateStrategy_LowSum_LessThanMin() {
        moblieCampaign.withStrategy(
                averageClickStrategy(avgBid, currency.getMinAutobudget().subtract(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                greaterThanOrEqualTo(currency.getMinAutobudget()))));
    }

    @Test
    public void validateStrategy_MinSum() {
        moblieCampaign.withStrategy(averageClickStrategy(avgBid, currency.getMinAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxSum() {
        moblieCampaign.withStrategy(averageClickStrategy(avgBid, currency.getMaxAutobudget()));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighSum_GreaterThanMax() {
        moblieCampaign.withStrategy(
                averageClickStrategy(avgBid, currency.getMaxAutobudget().add(new BigDecimal("0.1"))));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.SUM)),
                lessThanOrEqualTo(currency.getMaxAutobudget()))));
    }

    /**
     * Ниже проверка средней цены клика за неделю (AvgBid)
     */

    @Test
    public void validateStrategy_LowAvgBid_LessThanMin() {
        moblieCampaign.withStrategy(
                averageClickStrategy(constants.getMinAvgPrice().subtract(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                greaterThanOrEqualTo(constants.getMinAvgPrice()))));
    }

    @Test
    public void validateStrategy_MinAvgBid() {
        moblieCampaign.withStrategy(averageClickStrategy(constants.getMinAvgPrice(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_MaxAvgBid() {
        moblieCampaign.withStrategy(averageClickStrategy(currency.getMaxAutobudgetBid(), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_HighAvgBid_GreaterThanMax() {
        moblieCampaign.withStrategy(
                averageClickStrategy(currency.getMaxAutobudgetBid().add(new BigDecimal("0.1")), null));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.AVG_BID)),
                lessThanOrEqualTo(currency.getMaxAutobudgetBid()))));
    }

    /**
     * Ниже проверка средней цены клика (AvgBid) с недельным бюджетом (Sum)
     */

    @Test
    public void validateStrategy_AvgBidEqualToSum() {
        moblieCampaign.withStrategy(averageClickStrategy(new BigDecimal("500"), new BigDecimal("500")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateStrategy_AvgBidLowerThanSum() {
        moblieCampaign.withStrategy(averageClickStrategy(new BigDecimal("500"), new BigDecimal("500.01")));
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(moblieCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
