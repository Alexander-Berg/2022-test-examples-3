package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Прочие тесты на валидацию стратегии
 */
@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorTest {
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

    @Parameterized.Parameter
    public CampaignType campaignType;
    private CampaignValidationContainer campaignValidationContainer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.CPM_BANNER}
        });
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo);
        currency = clientService.getWorkCurrency(clientInfo.getClientId());
        campaign = (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
        campaignValidationContainer = CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L));
    }

    @Test
    public void strategy_NotNull() {
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(null);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY)),
                notNull())));
    }

    @Test
    public void strategyName_NotNull() {
        DbStrategy dbStrategy = (DbStrategy) defaultStrategy().withStrategyName(null);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_NAME)), notNull())));
    }

    @Test
    public void strategyName_CorrespondsToCampaign() {
        DbStrategy dbStrategy = defaultAutobudgetStrategy();
        campaign.withStrategy(dbStrategy);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction,
                campaign, emptySet(), Set.of(CampOptionsStrategy.values()), emptySet(),
                mock(StrategyValidatorConstants.class), ImmutableSet.of(), campaignValidationContainer, null)
                .apply(campaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_NAME)), inconsistentStrategyToCampaignType())));
    }

    @Test
    public void platform_CorrespondsToCampaign() {
        DbStrategy dbStrategy = defaultAutobudgetStrategy();
        campaign.withStrategy(dbStrategy);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction,
                campaign, Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()), emptySet(),
                mock(StrategyValidatorConstants.class), ImmutableSet.of(), campaignValidationContainer, null)
                .apply(campaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.PLATFORM)), inconsistentStrategyToCampaignType())));
    }

    @Test
    public void strategyData_NotNull() {
        DbStrategy dbStrategy = (DbStrategy) defaultStrategy().withStrategyData(null);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA)), notNull())));
    }

    @Test
    public void strategyData_DoNotValidateWhereNameIsInvalid() {
        BigDecimal min = currency.getMinAutobudget();
        DbStrategy dbStrategy = averageClickStrategy(new BigDecimal("100"), min.subtract(BigDecimal.ONE));
        campaign.withStrategy(dbStrategy);
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction,
                campaign, emptySet(), Set.of(CampOptionsStrategy.values()), Set.of(CampaignsPlatform.values()),
                mock(StrategyValidatorConstants.class), ImmutableSet.of(), campaignValidationContainer, null)
                .apply(campaign);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_NAME)), inconsistentStrategyToCampaignType())));
        assertThat("нет ошибки про некорректные данные стратегии", vr.flattenErrors(), hasSize(1));
    }

    private ValidationResult<CampaignWithCustomStrategy, Defect> validate(DbStrategy dbStrategy) {
        campaign.withStrategy(dbStrategy);
        return new CampaignWithCustomStrategyValidator(currency,
                emptySet(),
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction,
                campaign, Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()), emptySet(),
                new CommonStrategyValidatorConstants(currency), ImmutableSet.of(), campaignValidationContainer, null).apply(campaign);
    }
}
