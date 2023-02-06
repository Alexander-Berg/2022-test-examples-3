package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageAdGroupPriceRestrictions;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTERS_AVAILABLE_GOALS;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoFrontpageNoDesktopImmersionsInRegions;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachDbStrategy;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotLessThan;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorCpmYndxFrontpageCampaignTest {

    private static final int TEST_SHARD = 2;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private static LocalDateTime now = LocalDateTime.now();

    private Supplier<List<BannerWithSystemFields>> getCampaignBannersSupplier = Collections::emptyList;
    private Supplier<List<AdGroupSimple>> campaignAdGroupsSupplier = Collections::emptyList;
    private Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction =
            banners -> Collections.emptyList();

    private Currency currency;
    private CpmYndxFrontpageCampaign campaign;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        currency = clientService.getWorkCurrency(clientInfo.getClientId());
        campaign = (CpmYndxFrontpageCampaign) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                {"autoBudgetMaxReachStrategy", defaultAutobudgetMaxReachDbStrategy()},
                {"autoBudgetMaxImpressionsStrategy", defaultAutobudgetMaxImpressionsDbStrategy()},
                {"autoBudgetMaxReachCustomPeriodStrategy", defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.plusDays(1))},
                {"autoBudgetMaxImpressionsCustomPeriodStrategy", defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(now.plusDays(1))},
        });
    }

    @Test
    public void maxReachStrategyAvgPrice_NoDefects() {
        BigDecimal avgCpm = dbStrategy.getStrategyData().getAvgCpm();
        CpmYndxFrontpageAdGroupPriceRestrictions restrictions =
                buildRestriction(avgCpm.subtract(new BigDecimal("30")), avgCpm.subtract(new BigDecimal("20")));
        var vr = validate(dbStrategy, restrictions);
        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void maxReachStrategyAvgPrice_LessThanMin() {
        BigDecimal avgCpm = dbStrategy.getStrategyData().getAvgCpm();
        BigDecimal minPriceError = avgCpm.add(new BigDecimal("20"));
        BigDecimal minPriceWarning = avgCpm.add(new BigDecimal("30"));
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildRestriction(minPriceError, minPriceWarning);
        var vr = validate(dbStrategy, cpmYndxFrontpageAdGroupPriceRestrictions);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPM),
                invalidValueCpmNotLessThan(Money.valueOf(minPriceError.doubleValue(), CurrencyCode.RUB)))));
        GeoTree globalGeoTree = geoTreeFactory.getGlobalGeoTree();
        assertThat(vr, hasWarningWithDefinition(validationError(getFieldPath(StrategyData.AVG_CPM),
                geoFrontpageNoDesktopImmersionsInRegions(List.of(globalGeoTree.getRegion(Region.RUSSIA_REGION_ID))))));
    }

    private ValidationResult<CampaignWithCustomStrategy, Defect> validate(
            DbStrategy dbStrategy,
            CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions) {
        campaign.withStrategy(dbStrategy);
        return new CampaignWithCustomStrategyValidator(currency,
                CAMPAIGN_COUNTERS_AVAILABLE_GOALS,
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction, campaign, Set.of(StrategyName.values()),
                Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                new CommonStrategyValidatorConstants(currency), ImmutableSet.of(),
                CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L)),
                cpmYndxFrontpageAdGroupPriceRestrictions).apply(campaign);
    }

    private Path getFieldPath(ModelProperty<StrategyData, ?> strategyDataProperty) {
        return path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY_DATA),
                field(strategyDataProperty));
    }

    private CpmYndxFrontpageAdGroupPriceRestrictions buildRestriction(BigDecimal minPriceError,
                                                                      BigDecimal minPriceWarning) {
        return new CpmYndxFrontpageAdGroupPriceRestrictions(minPriceError, null)
                .withMinPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                        Map.of(Region.RUSSIA_REGION_ID, minPriceWarning)))
                .withRegionsById(geoTreeFactory.getGlobalGeoTree().getRegions())
                .withClientCurrency(CurrencyRub.getInstance());
    }
}
