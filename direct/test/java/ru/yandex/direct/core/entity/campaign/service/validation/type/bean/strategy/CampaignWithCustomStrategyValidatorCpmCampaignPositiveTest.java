package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorCpmCampaignPositiveTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Supplier<List<BannerWithSystemFields>> getCampaignBannersSupplier = Collections::emptyList;
    private Supplier<List<AdGroupSimple>> campaignAdGroupsSupplier = Collections::emptyList;
    private Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction =
            banners -> Collections.emptyList();


    private CpmBannerCampaign cpmBannerCampaign;

    private static LocalDateTime now = LocalDateTime.now();

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;
    private Currency currency;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                {"defaultCpm", defaultCpmStrategy()},
                {"autobudgetMaxReach", defaultAutobudgetMaxReachDbStrategy()},
                {"autobudgetMaxReachCustomPeriodDbStrategy", defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.plusDays(1))},
                {"autobudgetMaxImpressionsDbStrategy", defaultAutobudgetMaxImpressionsDbStrategy()},
                {"autobudgetMaxImpressionsCustomPeriodDbStrategy", defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(now.plusDays(1))},

        });
    }

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        cpmBannerCampaign = (CpmBannerCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
    }

    @Test
    public void checkValidatedSuccessfully() {
        cpmBannerCampaign.withStrategy(dbStrategy);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction, cpmBannerCampaign,
                Set.of(StrategyName.values()),
                Set.of(CampOptionsStrategy.values()), Set.of(CampaignsPlatform.values()),
                new CommonStrategyValidatorConstants(currency), ImmutableSet.of(),
                CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L)), null)
                .apply(cpmBannerCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

}
