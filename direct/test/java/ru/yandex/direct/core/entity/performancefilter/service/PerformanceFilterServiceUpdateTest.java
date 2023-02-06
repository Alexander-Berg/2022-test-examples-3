package ru.yandex.direct.core.entity.performancefilter.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.utils.PerformanceFilterUtils;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.math.BigDecimal.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.ADD_DEFAULT_SITE_FILTER_CONDITION_ENABLED;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_ROI;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class PerformanceFilterServiceUpdateTest {

    private static final Integer INIT_AUTOBUDGET_PRIORITY = 3;
    private static final BigDecimal INIT_PRICE_CPA = valueOf(101L);
    private static final BigDecimal INIT_PRICE_CPC = valueOf(100L);
    private static final Integer NEW_AUTOBUDGET_PRIORITY = 5;
    private static final BigDecimal NEW_PRICE_CPA = valueOf(300L);
    private static final BigDecimal NEW_PRICE_CPC = valueOf(400L);

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterValidationService performanceFilterValidationService;
    @Autowired
    private AutobudgetAlertService autobudgetAlertService;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerCommonRepository bannerCommonRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private LogPriceService logPriceService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private OldBannerRepository bannerRepository;

    private PerformanceFilterService performanceFilterService;

    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long operatorUid;
    private Long adGroupId;
    private Integer shard;
    private Long bannerId;
    private Long filterId;
    private Long campaignId;
    private PerformanceFilter beforeFilter;
    private PerformanceAdGroup beforeAdGroup;
    private PerformanceFilterInfo filterInfo;

    @Before
    public void before() throws Exception {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceFilter performanceFilter =
                defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                        .withAutobudgetPriority(INIT_AUTOBUDGET_PRIORITY)
                        .withPriceCpc(INIT_PRICE_CPC)
                        .withPriceCpa(INIT_PRICE_CPA);
        filterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(performanceFilter);
        steps.performanceFilterSteps().addPerformanceFilter(filterInfo);

        filterId = filterInfo.getFilterId();
        clientId = filterInfo.getClientId();
        clientInfo = filterInfo.getClientInfo();
        operatorUid = clientInfo.getUid();
        campaignId = filterInfo.getCampaignId();
        adGroupId = filterInfo.getAdGroupId();
        shard = filterInfo.getShard();
        beforeFilter = performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        beforeAdGroup = (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeInfo.getCreativeId());
        bannerId = bannerRepository.addBanners(shard, singletonList(banner)).get(0);
        OldBanner beforeBanner = bannerRepository.getBanners(shard, singleton(bannerId)).get(0);

        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        PpcProperty<Boolean> enabledProperty = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(ADD_DEFAULT_SITE_FILTER_CONDITION_ENABLED)).thenReturn(enabledProperty);
        when(enabledProperty.getOrDefault(false)).thenReturn(true);
        performanceFilterService = new PerformanceFilterService(
                performanceFilterValidationService,
                autobudgetAlertService,
                performanceFilterRepository,
                adGroupRepository,
                bannerCommonRepository,
                shardHelper,
                logPriceService,
                clientService,
                featureService,
                ppcPropertiesSupport
        );

        assumeThat(beforeAdGroup.getStatusModerate(), is(StatusModerate.YES));
        assumeThat(beforeAdGroup.getLastChange(), lessThan(LocalDateTime.now().minusMinutes(5)));
        assumeThat(beforeAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
        assumeThat((beforeAdGroup).getStatusBLGenerated(), is(StatusBLGenerated.YES));
        assumeThat(beforeBanner.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void updatePerformanceFilters_success_withoutConditionChanges() {
        //Исходные данные
        String newName = "New filter name.";
        TargetFunnel newTargetFunnel = TargetFunnel.PRODUCT_PAGE_VISIT;
        boolean newIsSuspended = true;
        BigDecimal newPriceCpa = BigDecimal.valueOf(333L);
        BigDecimal newPriceCpc = BigDecimal.valueOf(444L);
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withName(newName)
                .withTargetFunnel(newTargetFunnel)
                .withIsSuspended(newIsSuspended)
                .withPriceCpa(newPriceCpa)
                .withPriceCpc(newPriceCpc);

        //Ожидаемый после апдейта фильтр
        // CPC не меняется, т.к. стратегия averageCpaPerCampStrategy (DIRECT-122355)
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(filterId)
                .withName(newName)
                .withTargetFunnel(newTargetFunnel)
                .withIsSuspended(newIsSuspended)
                .withPriceCpa(newPriceCpa)
                .withPriceCpc(INIT_PRICE_CPC)
                .withIsDeleted(false);

        //Выполняем изменение фильтра
        MassResult<Long> massResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));
        assumeThat(massResult, isFullySuccessful());

        //Сверяем ожидания и реальность
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        PerformanceAdGroup actualAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        OldBanner actualBanner = bannerRepository.getBanners(shard, singleton(bannerId)).get(0);
        List<Long> updatedIds = mapList(massResult.getResult(), Result::getResult);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(actualFilter)
                    .is(matchedBy(beanDiffer(expectedFilter)
                            .useCompareStrategy(onlyExpectedFields()
                                    .forFields(newPath("priceCpa"), newPath("priceCpc"))
                                    .useDiffer(new BigDecimalDiffer()))));
            //Проверяем бизнес-логику
            // DIRECT-92377 #1 - НЕ должно примениться
            sa.assertThat(updatedIds).containsExactlyInAnyOrder(filterId);
            // DIRECT-92377 #2 - НЕ должно примениться
            sa.assertThat(actualAdGroup.getStatusBLGenerated()).isNotEqualTo(StatusBLGenerated.PROCESSING);
            // DIRECT-92377 #3 - должно примениться
            sa.assertThat(beforeFilter.getLastChange()).isBefore(actualFilter.getLastChange());
            // DIRECT-92377 #4 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            sa.assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            // DIRECT-92377 #5 - должно примениться
            sa.assertThat(beforeAdGroup.getLastChange()).isBefore(actualAdGroup.getLastChange());
            // DIRECT-92377 #6 - должно примениться
            sa.assertThat(actualFilter.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updatePerformanceFilters_success_whenConditionChangedRenew() {
        //Исходные данные
        List<PerformanceFilterCondition> changedConditions = otherFilterConditions();
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withConditions(changedConditions);

        //Выполняем изменение фильтра
        MassResult<Long> updateMassResult =
                performanceFilterService
                        .updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        //Сверяем ожидания и реальность
        PerformanceFilter oldActualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        PerformanceAdGroup actualAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        OldBanner actualBanner = bannerRepository.getBanners(clientInfo.getShard(), singleton(bannerId)).get(0);
        List<Long> resultIds = mapList(updateMassResult.getResult(), Result::getResult);
        Long newFilterId = resultIds.get(0);
        PerformanceFilter newActualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(newFilterId)).get(0);
        List<PerformanceFilterCondition> expectedConditions = otherFilterConditions();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(PerformanceFilterUtils.validAndEqual(newActualFilter.getConditions(), expectedConditions))
                    .isTrue();
            sa.assertThat(resultIds).hasSize(1);
            //Проверяем бизнес-логику
            // DIRECT-92377 #1 - должно примениться
            sa.assertThat(newFilterId).isNotEqualTo(filterId);
            sa.assertThat(oldActualFilter.getIsDeleted()).isTrue();
            // DIRECT-92377 #2 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.PROCESSING);
            // DIRECT-92377 #3 - должно примениться
            sa.assertThat(beforeFilter.getLastChange()).isBefore(oldActualFilter.getLastChange());
            // DIRECT-92377 #4 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            sa.assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            // DIRECT-92377 #5 - должно примениться
            sa.assertThat(beforeAdGroup.getLastChange()).isBefore(actualAdGroup.getLastChange());
            // DIRECT-92377 #6 - должно примениться
            sa.assertThat(oldActualFilter.getStatusBsSynced()).isNotEqualTo(StatusBsSynced.NO);
        });
    }


    @Test
    public void updatePerformanceFilters_success_whenConditionChangedUpdateByFeature() {
        //Исходные данные
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);
        List<PerformanceFilterCondition> changedConditions = otherFilterConditions();
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withConditions(changedConditions);

        //Выполняем изменение фильтра
        MassResult<Long> updateMassResult =
                performanceFilterService
                        .updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        //Сверяем ожидания и реальность
        PerformanceAdGroup actualAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        OldBanner actualBanner = bannerRepository.getBanners(clientInfo.getShard(), singleton(bannerId)).get(0);
        List<Long> resultIds = mapList(updateMassResult.getResult(), Result::getResult);
        Long receivedFilterId = resultIds.get(0);
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        List<PerformanceFilterCondition> expectedConditions = otherFilterConditions();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(PerformanceFilterUtils.validAndEqual(actualFilter.getConditions(), expectedConditions))
                    .as("actual conditions equal expected conditions").isTrue();
            sa.assertThat(resultIds).hasSize(1);
            //Проверяем бизнес-логику
            // DIRECT-92377 #1 - должно примениться
            sa.assertThat(receivedFilterId).isEqualTo(filterId);
            sa.assertThat(actualFilter.getIsDeleted()).isFalse();
            // DIRECT-92377 #2 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBLGenerated()).isEqualTo(StatusBLGenerated.PROCESSING);
            // DIRECT-92377 #3 - должно примениться
            sa.assertThat(beforeFilter.getLastChange()).isBefore(actualFilter.getLastChange());
            // DIRECT-92377 #4 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            sa.assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            // DIRECT-92377 #5 - должно примениться
            sa.assertThat(beforeAdGroup.getLastChange()).isBefore(actualAdGroup.getLastChange());
            // DIRECT-92377 #6 - должно примениться
            sa.assertThat(actualFilter.getStatusBsSynced()).isNotEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updatePerformanceFilters_success_withOnlyIsDeleteChanges() {
        //Исходные данные
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withIsDeleted(true);

        //Ожидаемый после апдейта фильтр
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(filterId)
                .withIsDeleted(true);

        //Выполняем изменение фильтра
        MassResult<Long> updateMassResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        //Сверяем ожидания и реальность
        PerformanceAdGroup actualAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        OldBanner actualBanner = bannerRepository.getBanners(clientInfo.getShard(), singleton(bannerId)).get(0);
        List<Long> updatedIds = mapList(updateMassResult.getResult(), Result::getResult);
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(updatedIds).hasSize(1);
            sa.assertThat(actualFilter)
                    .is(matchedBy(beanDiffer(expectedFilter)
                            .useCompareStrategy(onlyExpectedFields())));
            //Проверяем бизнес-логику
            // DIRECT-92377 #1 - НЕ должно примениться
            sa.assertThat(updatedIds).containsExactlyInAnyOrder(filterId);
            // DIRECT-92377 #2 - НЕ должно примениться
            sa.assertThat(actualAdGroup.getStatusBLGenerated()).isNotEqualTo(StatusBLGenerated.PROCESSING);
            // DIRECT-92377 #3 - должно примениться
            sa.assertThat(beforeFilter.getLastChange()).isBefore(actualFilter.getLastChange());
            // DIRECT-92377 #4 - должно примениться
            sa.assertThat(actualAdGroup.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            sa.assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            // DIRECT-92377 #5 - должно примениться
            sa.assertThat(beforeAdGroup.getLastChange()).isBefore(actualAdGroup.getLastChange());
            // DIRECT-92377 #6 - НЕ должно примениться
            sa.assertThat(actualFilter.getStatusBsSynced()).isNotEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updatePerformanceFilters_success_withNewPriceCpa() {
        BigDecimal newPriceCpa = BigDecimal.valueOf(333L);
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(newPriceCpa);

        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(newPriceCpa);

        MassResult<Long> updateMassResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        PerformanceAdGroup actualAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        OldBanner actualBanner = bannerRepository.getBanners(clientInfo.getShard(), singleton(bannerId)).get(0);
        List<Long> updatedIds = mapList(updateMassResult.getResult(), Result::getResult);
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(actualFilter)
                    .is(matchedBy(beanDiffer(expectedFilter)
                            .useCompareStrategy(onlyExpectedFields()
                                    .forFields(newPath("priceCpa"), newPath("priceCpc"))
                                    .useDiffer(new BigDecimalDiffer()))));
            //Проверяем бизнес-логику
            // DIRECT-92377 #1 - НЕ должно примениться
            sa.assertThat(updatedIds).as("filterId").containsExactlyInAnyOrder(filterId);
            // DIRECT-92377 #2 - НЕ должно примениться
            sa.assertThat(actualAdGroup.getStatusBLGenerated())
                    .as("AdGroup StatusBLGenerated").isNotEqualTo(StatusBLGenerated.PROCESSING);
            // DIRECT-92377 #3 - должно примениться
            sa.assertThat(beforeFilter.getLastChange())
                    .as("Filter LastChange").isBefore(actualFilter.getLastChange());
            // DIRECT-92377 #4 - НЕ должно примениться
            sa.assertThat(actualAdGroup.getStatusBsSynced())
                    .as("AdGroup StatusBsSynced").isEqualTo(StatusBsSynced.YES);
            sa.assertThat(actualBanner.getStatusBsSynced())
                    .as("Banner StatusBsSynced").isEqualTo(StatusBsSynced.YES);
            // DIRECT-92377 #5 - НЕ должно примениться
            sa.assertThat(beforeAdGroup.getLastChange())
                    .as("AdGroup LastChange").isEqualTo(actualAdGroup.getLastChange());
            // DIRECT-92377 #6 - должно примениться
            sa.assertThat(actualFilter.getStatusBsSynced())
                    .as("Filter StatusBsSynced").isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updatePerformanceFilters_failure_whenNewNameIsTooLong() {
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withName(StringUtils.repeat('A', 101));
        MassResult<Long> massResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        MatcherAssert.assertThat(massResult.getValidationResult(), CoreMatchers.is(
                hasDefectDefinitionWith(validationError(path(index(0), field(PerformanceFilter.NAME)),
                        CollectionDefects.maxStringLength(100)))
        ));
    }

    @Test
    public void updatePerformanceFilters_failure_whenAccessCheckerReturnsError() {
        operatorUid = steps.clientSteps().createDefaultClient().getUid();

        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withName("New filter name");
        MassResult<Long> massResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));

        MatcherAssert.assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(PerformanceFilter.PID)),
                        AdGroupDefects.notFound()))
        );
    }

    private static Object[] parametrizedTestData_CheckPriceChanges() {
        return new Object[][]{
                {"AVG_CPA_PER_CAMP стратегия -> меняется только CPA", AUTOBUDGET_AVG_CPA_PER_CAMP,
                        NEW_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, NEW_PRICE_CPC,
                        INIT_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, INIT_PRICE_CPC},
                {"AVG_CPA_PER_FILTER стратегия -> меняется только CPA", AUTOBUDGET_AVG_CPA_PER_FILTER,
                        NEW_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, NEW_PRICE_CPC,
                        INIT_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, INIT_PRICE_CPC},
                {"AVG_CPC_PER_CAMP стратегия -> меняется только CPC", AUTOBUDGET_AVG_CPC_PER_CAMP,
                        NEW_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, NEW_PRICE_CPC,
                        INIT_AUTOBUDGET_PRIORITY, INIT_PRICE_CPA, NEW_PRICE_CPC},
                {"AVG_CPC_PER_FILTER стратегия -> меняется только CPC", AUTOBUDGET_AVG_CPC_PER_FILTER,
                        NEW_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, NEW_PRICE_CPC,
                        INIT_AUTOBUDGET_PRIORITY, INIT_PRICE_CPA, NEW_PRICE_CPC},
                {"AUTOBUDGET_ROI стратегия -> меняется только AUTOBUDGET_PRIORITY", AUTOBUDGET_ROI,
                        NEW_AUTOBUDGET_PRIORITY, NEW_PRICE_CPA, NEW_PRICE_CPC,
                        NEW_AUTOBUDGET_PRIORITY, INIT_PRICE_CPA, INIT_PRICE_CPC},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_CheckPriceChanges")
    @TestCaseName("{0}")
    public void updatePerformanceFilters_CheckPriceChanges(@SuppressWarnings("unused") String testDescription,
                                                           StrategyName campaignStrategy, Integer autobudgetPriority,
                                                           BigDecimal newPriceCpa, BigDecimal newPriceCpc,
                                                           Integer expectAutobudgetPriority,
                                                           BigDecimal expectPriceCpa, BigDecimal expectPriceCpc) {
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), campaignStrategy);

        //Исходные данные
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withAutobudgetPriority(autobudgetPriority)
                .withPriceCpa(newPriceCpa)
                .withPriceCpc(newPriceCpc);

        //Выполняем изменение фильтра
        MassResult<Long> massResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(changedFilter));
        assumeThat(massResult, isFullySuccessful());

        //Сверяем ожидания и реальность
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(actualFilter.getPriceCpa()).as("cpa price")
                    .isEqualByComparingTo(expectPriceCpa);
            sa.assertThat(actualFilter.getPriceCpc()).as("cpc price")
                    .isEqualByComparingTo(expectPriceCpc);
            sa.assertThat(actualFilter.getAutobudgetPriority()).as("autobudget priority")
                    .isEqualTo(expectAutobudgetPriority);
        });
    }

    @Test
    public void updatePerformanceFilters_changeConditions_siteFeed_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(emptyList());

        Long filterId = steps.performanceFilterSteps().addPerformanceFilter(shard, filter);

        steps.performanceFilterSteps()
                .setPerformanceFilterProperty(new PerformanceFilterInfo().withFilter(filter).withAdGroupInfo(adGroupInfo),
                        PerformanceFilter.CONDITIONS, List.of(defaultSiteFilterCondition().withParsedValue(true)));

        PerformanceFilterCondition condition =
                new PerformanceFilterCondition<>("price", Operator.RANGE, "[\"3000.00-100000.00\",\"111.00-222.00\"]")
                        .withParsedValue(Stream.of(
                                new DecimalRange("3000.00-100000.00"),
                                new DecimalRange("111.00-222.00")
                        ).collect(toList()));
        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withId(filterId)
                .withSource(Source.SITE)
                .withConditions(List.of(condition));

        MassResult<Long> massResult =
                performanceFilterService.updatePerformanceFilters(clientId, operatorUid, singletonList(expectedFilter));
        assumeThat(massResult, isFullySuccessful());

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withAdGroupIds(singleton(adGroupInfo.getAdGroupId()))
                .withoutDeleted()
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, queryFilter).get(0);
        List<PerformanceFilterCondition> expectedConditions = List.of(
                defaultSiteFilterCondition().withParsedValue(null), condition);
        assertThat(actualFilter.getConditions()).isEqualTo(expectedConditions);
    }
}
