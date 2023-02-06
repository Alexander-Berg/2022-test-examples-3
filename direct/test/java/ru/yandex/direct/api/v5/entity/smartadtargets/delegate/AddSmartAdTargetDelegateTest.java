package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.StringConditionOperatorEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.smartadtargets.AddRequest;
import com.yandex.direct.api.v5.smartadtargets.AddResponse;
import com.yandex.direct.api.v5.smartadtargets.AudienceEnum;
import com.yandex.direct.api.v5.smartadtargets.ConditionsArray;
import com.yandex.direct.api.v5.smartadtargets.SmartAdTargetAddItem;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.common.ConverterUtils;
import ru.yandex.direct.api.v5.common.GeneralUtil;
import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.smartadtargets.converter.AddRequestConverterService;
import ru.yandex.direct.api.v5.entity.smartadtargets.validation.SmartAdTargetValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterValidationService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.AUDIENCE_BY_FUNNEL;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.IS_AVAILABLE_CONDITION;
import static ru.yandex.direct.api.v5.entity.smartadtargets.delegate.TestUtils.getConditionsArray;
import static ru.yandex.direct.common.db.PpcPropertyNames.ADD_DEFAULT_SITE_FILTER_CONDITION_ENABLED;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.hotelPerformanceFilter;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class AddSmartAdTargetDelegateTest {
    @Autowired
    private Steps steps;
    @Autowired
    private AddRequestConverterService requestConverterService;
    @Autowired
    private ResultConverter resultConverter;
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
    private SmartAdTargetValidationService validationService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private AddSmartAdTargetDelegate delegate;
    private PerformanceFilterService performanceFilterService;

    private ClientInfo clientInfo;
    private Integer shard;

    private static AddRequest createRequest(PerformanceFilter filter) {
        AudienceEnum audience = ifNotNull(filter.getTargetFunnel(), AUDIENCE_BY_FUNNEL::get);
        Long averageCpc = convertToMicros(filter.getPriceCpc());
        Long averageCpa = convertToMicros(filter.getPriceCpa());
        YesNoEnum availableItemsOnly = Optional.ofNullable(filter.getConditions())
                .map(condition -> condition.stream().anyMatch(IS_AVAILABLE_CONDITION))
                .map(GeneralUtil::yesNoFromBool)
                .orElse(null);
        ConditionsArray conditionsArray = getConditionsArray(filter.getConditions());
        PriorityEnum priority = ifNotNull(filter.getAutobudgetPriority(), ConverterUtils::convertStrategyPriority);
        return new AddRequest()
                .withSmartAdTargets(
                        new SmartAdTargetAddItem()
                                .withAdGroupId(filter.getPid())
                                .withAudience(audience)
                                .withAvailableItemsOnly(availableItemsOnly)
                                .withAverageCpa(averageCpa)
                                .withAverageCpc(averageCpc)
                                .withName(filter.getName())
                                .withStrategyPriority(priority)
                                .withConditions(conditionsArray)
                );
    }

    private PerformanceFilter getNewFilter() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedInfo.getFeedId());
        return fillCommonParams(filter);
    }

    private static PerformanceFilter fillCommonParams(PerformanceFilter performanceFilter) {
        return performanceFilter
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withTab(PerformanceFilterTab.CONDITION);
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
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

        delegate = new AddSmartAdTargetDelegate(auth, validationService, performanceFilterService,
                requestConverterService, resultConverter, ppcPropertiesSupport, featureService);
    }

    @Test
    public void add_yandexMarketFilter_success() {
        PerformanceFilter expectedFilter = getNewFilter().withTab(PerformanceFilterTab.TREE);

        AddRequest request = createRequest(expectedFilter);
        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error in response");

        Long newFilterId = response.getAddResults().get(0).getId();
        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(newFilterId))
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, queryFilter).get(0);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void add_googleHotelFilter_success() {
        PerformanceFilter filter = hotelPerformanceFilter(null, null);
        FeedInfo feedInfo = steps.feedSteps().createFeed(clientInfo, filter.getFeedType(), filter.getBusinessType());
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        PerformanceFilter expectedFilter = fillCommonParams(filter)
                .withPid(adGroupInfo.getAdGroupId())
                .withFeedId(feedInfo.getFeedId());

        AddRequest request = createRequest(expectedFilter);
        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error in response");

        Long newFilterId = response.getAddResults().get(0).getId();
        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(newFilterId))
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, queryFilter).get(0);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void add_withoutConditions_success() {
        PerformanceFilter filter = hotelPerformanceFilter(null, null);
        FeedInfo feedInfo = steps.feedSteps().createFeed(clientInfo, filter.getFeedType(), filter.getBusinessType());
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        fillCommonParams(filter);
        filter.withPid(adGroupInfo.getAdGroupId())
                .withFeedId(feedInfo.getFeedId())
                .withConditions(null);

        AddRequest request = createRequest(filter);
        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error in response");

        Long newFilterId = response.getAddResults().get(0).getId();
        PerformanceFiltersQueryFilter query = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(newFilterId))
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, query).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualFilter.getConditions()).as("Conditions").isEmpty();
            soft.assertThat(actualFilter.getTab()).as("Tab").isEqualTo(PerformanceFilterTab.ALL_PRODUCTS);
        });
    }

    @Test
    public void add_withEmptyConditions_success() {
        PerformanceFilter filter = hotelPerformanceFilter(null, null);
        FeedInfo feedInfo = steps.feedSteps().createFeed(clientInfo, filter.getFeedType(), filter.getBusinessType());
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        fillCommonParams(filter);
        filter.withPid(adGroupInfo.getAdGroupId())
                .withFeedId(feedInfo.getFeedId())
                .withConditions(emptyList());

        AddRequest request = createRequest(filter);
        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error in response");

        Long newFilterId = response.getAddResults().get(0).getId();
        PerformanceFiltersQueryFilter query = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(newFilterId))
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, query).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualFilter.getConditions()).as("Conditions").isEmpty();
            soft.assertThat(actualFilter.getTab()).as("Tab").isEqualTo(PerformanceFilterTab.ALL_PRODUCTS);
        });
    }

    @Test
    public void add_perfFilterForAdgroupWithSiteFeed_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withSource(Source.SITE)
                .withConditions(emptyList());
        fillCommonParams(filter);

        AddRequest request = createRequest(filter);
        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error in response");

        Long newFilterId = response.getAddResults().get(0).getId();
        PerformanceFiltersQueryFilter query = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(newFilterId))
                .build();
        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, query).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualFilter.getConditions()).as("Conditions").isEqualTo(List.of(
                    defaultSiteFilterCondition().withParsedValue(null))
            );
            soft.assertThat(actualFilter.getTab()).as("Tab").isEqualTo(PerformanceFilterTab.ALL_PRODUCTS);
        });
    }

    @Test
    public void add_withInconsistentOperator_failure() {
        PerformanceFilter filter = getNewFilter();

        AddRequest request = createRequest(filter);
        StreamEx.of(request.getSmartAdTargets())
                .flatMap(i -> StreamEx.of(i.getConditions()))
                .flatMap(i -> StreamEx.of(i.getItems()))
                .findFirst(c -> c.getOperator() == StringConditionOperatorEnum.IN_RANGE)
                .orElseThrow(IllegalStateException::new)
                .withOperator(StringConditionOperatorEnum.CONTAINS_ANY);

        ExceptionNotification expectedError = new ExceptionNotification()
                .withCode(5005)
                .withDetails("The operator used in the rules does not match the operand")
                .withMessage("Field set incorrectly");

        AddResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getAddResults().get(0).getErrors();
        checkState(errors.size() == 1);

        ExceptionNotification actualError = errors.get(0);
        assertThat(actualError)
                .as("expected error")
                .is(matchedBy(beanDiffer(expectedError).useCompareStrategy(onlyExpectedFields())));
    }

    @Test(expected = ApiValidationException.class)
    public void add_whenInconsistentAdGroupType_failure() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        PerformanceFilter performanceFilter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), null);
        AddRequest request = createRequest(performanceFilter);
        genericApiService.doAction(delegate, request);
    }

}
