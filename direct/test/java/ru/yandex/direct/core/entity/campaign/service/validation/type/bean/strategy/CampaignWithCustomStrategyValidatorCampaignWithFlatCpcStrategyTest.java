package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matcher;
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
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.flatCpcStrategyNotSupported;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.feature.FeatureName.FLAT_CPC_ADDING_DISABLED;
import static ru.yandex.direct.feature.FeatureName.FLAT_CPC_DISABLED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorCampaignWithFlatCpcStrategyTest {
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
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Supplier<List<BannerWithSystemFields>> getCampaignBannersSupplier = Collections::emptyList;
    private Supplier<List<AdGroupSimple>> campaignAdGroupsSupplier = Collections::emptyList;
    private Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction =
            banners -> Collections.emptyList();


    private Currency currency;
    private CampaignWithCustomStrategy campaign;
    private static DbStrategy flatCpcDbStrategy = (DbStrategy) defaultStrategy().withStrategy(null);
    private static DbStrategy notDefaultDbStrategy = defaultAutobudgetStrategy();

    private CampaignValidationContainer campaignValidationContainer;
    private ClientInfo clientInfo;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Parameterized.Parameter(2)
    public boolean warningOnFlatCpcStrategyUpdate;

    @Parameterized.Parameter(3)
    public Set<String> availableFeatures;

    @Parameterized.Parameter(4)
    public Matcher<ValidationResult<CampaignWithCustomStrategy, Defect>> expectedDefects;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClientAnotherShard();
        currency = clientService.getWorkCurrency(clientInfo.getClientId());

        campaignValidationContainer = CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][] {
                {"notFlatCpcStrategyAddWithFeaturesDisabled", defaultStrategy(), false, Collections.emptySet(), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyUpdateWithFeaturesDisabled", defaultStrategy(), true, Collections.emptySet(), hasNoErrorsAndWarnings()},
                {"flatCpcStrategyAddWithFeaturesDisabled", flatCpcDbStrategy, false, Collections.emptySet(), hasNoErrorsAndWarnings()},
                {"flatCpcStrategyUpdateWithFeaturesDisabled", flatCpcDbStrategy, true, Collections.emptySet(), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyAddWithFlatCpcDisabled", defaultStrategy(), false, ImmutableSet.of(FLAT_CPC_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyUpdateWithFlatCpcDisabled", defaultStrategy(), true, ImmutableSet.of(FLAT_CPC_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyAddWithFlatCpcAddingDisabled", defaultStrategy(), false, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyUpdateWithFlatCpcAddingDisabled", defaultStrategy(), true, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyAddWithBothFeatures", defaultStrategy(), false, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"notFlatCpcStrategyUpdateWithBothFeatures", defaultStrategy(), true, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()), hasNoErrorsAndWarnings()},
                {"flatCpcStrategyAddWithFlatCpcDisabled", flatCpcDbStrategy, false, ImmutableSet.of(FLAT_CPC_DISABLED.getName()),
                        hasDefectWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"flatCpcStrategyUpdateWithFlatCpcDisabled", flatCpcDbStrategy, true, ImmutableSet.of(FLAT_CPC_DISABLED.getName()),
                        hasDefectWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"flatCpcStrategyAddWithFlatCpcAddingDisabled", flatCpcDbStrategy, false, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()),
                        hasDefectWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"flatCpcStrategyUpdateWithFlatCpcAddingDisabled", flatCpcDbStrategy, true, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()),
                        hasWarningWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"flatCpcStrategyAddWithBothFeaturesEnabled", flatCpcDbStrategy, false, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()),
                        hasDefectWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"flatCpcStrategyUpdateWithBothFeaturesEnabled", flatCpcDbStrategy, true, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()),
                        hasDefectWithDefinition(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY)), flatCpcStrategyNotSupported()))},
                {"notDefaultStrategyAddWithFlatCpcDisabled", notDefaultDbStrategy, false, ImmutableSet.of(FLAT_CPC_DISABLED.getName()),
                        hasNoErrorsAndWarnings()},
                {"notDefaultStrategyAddWithFlatCpcAddingDisabled", notDefaultDbStrategy, false, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()),
                        hasNoErrorsAndWarnings()},
                {"notDefaultStrategyAddWithBothFeatures", notDefaultDbStrategy, false, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()),
                        hasNoErrorsAndWarnings()},
                {"notDefaultStrategyUpdateWithFlatCpcDisabled", notDefaultDbStrategy, true, ImmutableSet.of(FLAT_CPC_DISABLED.getName()),
                        hasNoErrorsAndWarnings()},
                {"notDefaultStrategyUpdateWithFlatCpcAddingDisabled", notDefaultDbStrategy, true, ImmutableSet.of(FLAT_CPC_ADDING_DISABLED.getName()),
                        hasNoErrorsAndWarnings()},
                {"notDefaultStrategyUpdateWithBothFeatures", notDefaultDbStrategy, true, ImmutableSet.of(FLAT_CPC_DISABLED.getName(), FLAT_CPC_ADDING_DISABLED.getName()),
                        hasNoErrorsAndWarnings()}
        });
    }

    @Test
    public void test_TextCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        campaign = (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(warningOnFlatCpcStrategyUpdate, dbStrategy);
        assertThat(vr, expectedDefects);

        if (vr.flattenErrors().size() > 0) {
            assertThat(vr.flattenErrors(), hasSize(1));
        }
    }

    @Test
    public void test_DynamicCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);

        campaign = (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(warningOnFlatCpcStrategyUpdate, dbStrategy);
        assertThat(vr, expectedDefects);

        if (vr.flattenErrors().size() > 0) {
            assertThat(vr.flattenErrors(), hasSize(1));
        }
    }

    @Test
    public void test_MobileContentCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        campaign = (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(warningOnFlatCpcStrategyUpdate, dbStrategy);
        assertThat(vr, expectedDefects);

        if (vr.flattenErrors().size() > 0) {
            assertThat(vr.flattenErrors(), hasSize(1));
        }
    }

    private ValidationResult<CampaignWithCustomStrategy, Defect> validate(boolean warningOnFlatCpcStrategyUpdate, DbStrategy dbStrategy) {
        campaign.withStrategy(dbStrategy);
        return new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction,
                campaign, Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()), Set.of(CampaignsPlatform.values()),
                new CommonStrategyValidatorConstants(currency), availableFeatures, campaignValidationContainer, null,
                false, mobileAppIds -> List.of(), appId -> null, warningOnFlatCpcStrategyUpdate,
                null).apply(campaign);
    }
}
