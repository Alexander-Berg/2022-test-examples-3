package ru.yandex.direct.core.entity.performancefilter.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAutobudgetAlertRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.ADD_DEFAULT_SITE_FILTER_CONDITION_ENABLED;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceFiltersAddOperationTest {
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
    @Autowired
    private TestAutobudgetAlertRepository testAutobudgetAlertRepository;
    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private PerformanceFilterService performanceFilterService;

    private CampaignInfo campaignInfo;
    private Long campaignId;
    private int shard;
    private Long feedId;
    private Long creativeId;
    private Long adGroupId;
    private Long bannerId;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long operatorUid;
    private OldBanner beforeBanner;

    @Before
    public void before() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();
        feedId = adGroupInfo.getFeedId();
        clientId = adGroupInfo.getClientId();
        clientInfo = adGroupInfo.getClientInfo();
        operatorUid = clientInfo.getUid();
        campaignId = adGroupInfo.getCampaignId();
        adGroupId = adGroupInfo.getAdGroupId();
        shard = adGroupInfo.getShard();
        PerformanceAdGroup beforeAdGroup =
                (PerformanceAdGroup) adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        creativeId = creativeInfo.getCreativeId();
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeInfo.getCreativeId());
        bannerId = bannerRepository.addBanners(shard, singletonList(banner)).get(0);
        beforeBanner = bannerRepository.getBanners(shard, singleton(bannerId)).get(0);

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
    public void addPerformanceFilters() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId);
        Long filterId = addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceFilter actualFilter = getFiltersByPerfFilterIds(shard, singletonList(filterId)).get(0);

        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withPerfFilterId(filterId)
                .withStatusBsSynced(StatusBsSynced.NO);

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("lastChange")).useMatcher(approximatelyNow())
                .forFields(newPath("priceCpa"), newPath("priceCpc")).useDiffer(new BigDecimalDiffer());

        compareFilters(actualFilter, expectedFilter, compareStrategy);
    }

    @Test
    public void addPerformanceFilters_addFilterTab() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId).withTab(PerformanceFilterTab.TREE);
        Long filterId = addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceFilterTab actualFilterTab = getPerformanceFilterTab(shard, filterId);
        assertThat(actualFilterTab, is(PerformanceFilterTab.TREE));
    }

    @Test
    public void addPerformanceFilters_freezeAutobudgetAlerts() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId).withTab(PerformanceFilterTab.TREE);
        testAutobudgetAlertRepository.addAutobudgetAlert(shard, campaignId);

        addPerformanceFilterAndCheckIsSuccessful(filter);

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(shard, campaignId);
    }

    @Test
    public void addPerformanceFilters_noFreezeAutobudgetAlerts_draftAdGroup() {
        PerformanceAdGroup draftAdGroup = activePerformanceAdGroup(null, feedId)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(draftAdGroup, campaignInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedId)
                .withTab(PerformanceFilterTab.TREE);
        testAutobudgetAlertRepository.addAutobudgetAlert(shard, campaignId);

        addPerformanceFilterAndCheckIsSuccessful(filter);

        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(shard, campaignId);
    }

    @Test
    public void addPerformanceFilters_dontAddFilterTab_whenFilterTabIsNull() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId).withTab(null);
        Long filterId = addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceFilterTab actualFilterTab = getPerformanceFilterTab(shard, filterId);
        PerformanceFilterTab expectedFilterTab = PerformanceFilterTab.CONDITION;
        assertThat(actualFilterTab, beanDiffer(expectedFilterTab).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addPerformanceFilters_updateAdGroupStatuses() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withIsSuspended(false);
        addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withLastChange(LocalDateTime.now());

        checkAdGroup(adGroupId, expectedAdGroup);
    }

    @Test
    public void addPerformanceFilters_dontUpdateAdGroupStatusBLGenerated_whenFilterIsSuspended() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withIsSuspended(true);
        addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusBLGenerated(StatusBLGenerated.YES)
                .withLastChange(LocalDateTime.now());

        checkAdGroup(adGroupId, expectedAdGroup);
    }

    @Test
    public void addPerformanceFilters_dontUpdateAdGroupStatuses_whenAdGroupIsDraft() {
        PerformanceAdGroup draftAdGroup = activePerformanceAdGroup(null, feedId)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.SENDING)
                .withStatusBLGenerated(StatusBLGenerated.NO)
                .withLastChange(LocalDateTime.now().minusDays(1));
        Long draftAdGroupId = steps.adGroupSteps().createAdGroup(draftAdGroup, campaignInfo).getAdGroupId();

        PerformanceFilter filter = defaultPerformanceFilter(draftAdGroupId, feedId);
        addPerformanceFilterAndCheckIsSuccessful(filter);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.SENDING)
                .withStatusBLGenerated(StatusBLGenerated.NO)
                .withLastChange(LocalDateTime.now());

        checkAdGroup(draftAdGroupId, expectedAdGroup);
    }

    @Test
    public void addPerformanceFilters_updateBannerStatusBsSynced() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId);
        addPerformanceFilterAndCheckIsSuccessful(filter);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChange(beforeBanner.getLastChange());

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addPerformanceFilters_dontUpdateBannerStatusBsSynced_whenAdGroupIsDraft() {
        PerformanceAdGroup draftAdGroup = activePerformanceAdGroup(null, feedId)
                .withStatusModerate(StatusModerate.NEW);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(draftAdGroup, campaignInfo);
        Long draftAdGroupId = adGroupInfo.getAdGroupId();

        LocalDateTime lastChange = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS);

        OldPerformanceBanner banner = activePerformanceBanner(campaignId, draftAdGroupId, creativeId)
                .withStatusBsSynced(StatusBsSynced.SENDING)
                .withLastChange(lastChange);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        PerformanceFilter filter = defaultPerformanceFilter(draftAdGroupId, feedId);
        addPerformanceFilterAndCheckIsSuccessful(filter);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withStatusBsSynced(StatusBsSynced.SENDING)
                .withLastChange(lastChange);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addPerformanceFilters_failure_whenNameIsTooLong() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withName(StringUtils.repeat('A', 101));

        MassResult<Long> massResult = addPerformanceFilters(clientId, singletonList(filter));

        assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0),
                        field(PerformanceFilter.NAME)), CollectionDefects.maxStringLength(100))));
    }

    @Test
    public void addPerformanceFilters_failure_whenAccessCheckerReturnsError() {
        testCampaignRepository.archiveCampaign(shard, campaignId);

        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId);
        MassResult<Long> massResult = addPerformanceFilters(clientId, singletonList(filter));

        assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0),
                        field(PerformanceFilter.PID)), archivedCampaignModification())));
    }

    @Test
    public void addPerformanceFilters_failure_whenEmptyConditions_andTabIsNotAllProducts() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withTab(PerformanceFilterTab.CONDITION)
                .withConditions(emptyList());

        MassResult<Long> massResult = addPerformanceFilters(clientId, singletonList(filter));

        assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0),
                        field(PerformanceFilter.CONDITIONS)), CollectionDefects.notEmptyCollection())));
    }

    @Test
    public void addPerformanceFilters_success_whenEmptyConditions_andTabIsAllProducts() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withConditions(emptyList());

        MassResult<Long> massResult = addPerformanceFilters(clientId, singletonList(filter));

        assertThat(massResult.getValidationResult(), hasNoErrorsAndWarnings());
    }

    @Test
    public void addPerformanceFilters_success_whenSourceSite_hasDefaultCondition() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(emptyList());

        Long filterId = addPerformanceFilterAndCheckIsSuccessful(filter);

        assertThat(getFiltersByPerfFilterIds(shard, singletonList(filterId)).get(0).getConditions(),
                beanDiffer(List.of(defaultSiteFilterCondition().withParsedValue(null))));
    }

    private void checkAdGroup(Long adGroupId, PerformanceAdGroup expectedAdGroup) {
        PerformanceAdGroup actualAdGroup = (PerformanceAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(
                onlyExpectedFields().forFields(newPath("lastChange")).useMatcher(approximatelyNow())));
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldPerformanceBanner actualBanner = (OldPerformanceBanner) bannerRepository
                .getBanners(shard, singletonList(bannerId)).get(0);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private List<PerformanceFilter> getFiltersByPerfFilterIds(int shard, List<Long> perfFilterIds) {
        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(perfFilterIds)
                .build();
        return performanceFilterRepository.getFilters(shard, queryFilter);
    }

    private PerformanceFilterTab getPerformanceFilterTab(int shard, Long filterId) {
        List<PerformanceFilter> filters = performanceFilterRepository.getFiltersById(shard, singleton(filterId));
        PerformanceFilter filter = filters.get(0);
        return filter.getTab();
    }

    private Long addPerformanceFilterAndCheckIsSuccessful(PerformanceFilter filter) {
        List<Long> filterIds = addPerformanceFiltersAndCheckIsSuccessful(singletonList(filter));
        return filterIds.get(0);
    }

    private List<Long> addPerformanceFiltersAndCheckIsSuccessful(List<PerformanceFilter> filters) {
        MassResult<Long> massResult = addPerformanceFilters(campaignInfo.getClientId(), filters);
        assumeThat(massResult, isFullySuccessful());
        return mapList(massResult.getResult(), Result::getResult);
    }

    private MassResult<Long> addPerformanceFilters(ClientId clientId, List<PerformanceFilter> filters) {
        return performanceFilterService.createPartialAddOperation(clientId, operatorUid, filters).prepareAndApply();
    }
}
