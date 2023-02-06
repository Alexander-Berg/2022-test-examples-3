package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.smartadtargets.AudienceEnum;
import com.yandex.direct.api.v5.smartadtargets.SmartAdTargetUpdateItem;
import com.yandex.direct.api.v5.smartadtargets.UpdateRequest;
import com.yandex.direct.api.v5.smartadtargets.UpdateResponse;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
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
import ru.yandex.direct.api.v5.entity.smartadtargets.converter.UpdateRequestConverterService;
import ru.yandex.direct.api.v5.entity.smartadtargets.validation.SmartAdTargetValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
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
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterValidationService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.AUDIENCE_BY_FUNNEL;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.FACTORY;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.IS_AVAILABLE_CONDITION;
import static ru.yandex.direct.api.v5.entity.smartadtargets.delegate.TestUtils.getConditionsArray;
import static ru.yandex.direct.common.db.PpcPropertyNames.ADD_DEFAULT_SITE_FILTER_CONDITION_ENABLED;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultFilterConditions;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class UpdateSmartAdTargetDelegateTest {
    @Autowired
    private Steps steps;
    @Autowired
    private UpdateRequestConverterService requestConverterService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private SmartAdTargetValidationService validationService;
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
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private UpdateSmartAdTargetDelegate delegate;
    private PerformanceFilterService performanceFilterService;

    private Integer shard;
    private Long filterId;
    private Long adGroupId;
    private BigDecimal initPriceCpc;
    private BigDecimal initPriceCpa;
    private PerformanceFilterInfo filterInfo;
    private ClientInfo clientInfo;

    private static UpdateRequest createRequest(Long filterId, PerformanceFilter filter) {
        AudienceEnum audience = ifNotNull(filter.getTargetFunnel(), AUDIENCE_BY_FUNNEL::get);
        JAXBElement<Long> averageCpc =
                FACTORY.createSmartAdTargetUpdateItemAverageCpc(convertToMicros(filter.getPriceCpc()));
        JAXBElement<Long> averageCpa =
                FACTORY.createSmartAdTargetUpdateItemAverageCpa(convertToMicros(filter.getPriceCpa()));
        YesNoEnum availableItemsOnly = Optional.ofNullable(filter.getConditions())
                .map(condition -> condition.stream().anyMatch(IS_AVAILABLE_CONDITION))
                .map(GeneralUtil::yesNoFromBool)
                .orElse(null);
        var conditionsArray =
                FACTORY.createSmartAdTargetUpdateItemConditions(getConditionsArray(filter.getConditions()));
        PriorityEnum priority = ifNotNull(filter.getAutobudgetPriority(), ConverterUtils::convertStrategyPriority);
        return new UpdateRequest()
                .withSmartAdTargets(
                        new SmartAdTargetUpdateItem()
                                .withId(filterId)
                                .withName(filter.getName())
                                .withAudience(audience)
                                .withAverageCpc(averageCpc)
                                .withAverageCpa(averageCpa)
                                .withStrategyPriority(priority)
                                .withAvailableItemsOnly(availableItemsOnly)
                                .withConditions(conditionsArray)
                );
    }

    @Before
    public void before() {
        filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        initPriceCpc = filterInfo.getFilter().getPriceCpc();
        initPriceCpa = filterInfo.getFilter().getPriceCpa();
        adGroupId = filterInfo.getAdGroupId();
        filterId = filterInfo.getFilterId();
        shard = filterInfo.getShard();
        clientInfo = filterInfo.getClientInfo();
        Long uid = clientInfo.getUid();
        ClientId clientId = clientInfo.getClientId();

        ApiUser user = new ApiUser()
                .withUid(uid)
                .withClientId(clientId);
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

        delegate = new UpdateSmartAdTargetDelegate(auth, performanceFilterService, requestConverterService,
                resultConverter, validationService, ppcPropertiesSupport, featureService);
    }


    @Test
    public void update_changePropertiesWhenCpaStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        PerformanceFilter startFilter = getActualFilter(adGroupId);
        String newName = "new filter name";
        long newCpa = startFilter.getPriceCpa().longValue() + 100L;

        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withName(newName)
                .withPriceCpc(initPriceCpc)
                .withPriceCpa(BigDecimal.valueOf(newCpa))
                .withTargetFunnel(TargetFunnel.SAME_PRODUCTS);

        UpdateRequest request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        PerformanceFilter actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter);
    }

    /**
     * Возвращает актуальный фильтр по id-группы объявлений.
     */
    private PerformanceFilter getActualFilter(Long adGroupId) {
        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withAdGroupIds(singleton(adGroupId))
                .withoutDeleted()
                .build();
        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(shard, queryFilter);
        checkState(filters.size() == 1, "Unexpected count of the filters");
        return filters.get(0);
    }

    @Test
    public void update_whenFilterNotExist_failure() {
        long notExistFilterId = Integer.MAX_VALUE - 1L;
        UpdateRequest request = createRequest(notExistFilterId, defaultPerformanceFilter(null, null));
        UpdateResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }

    @Test(expected = ApiValidationException.class)
    public void suspend_whenIdsIsTooMany_failure() {
        UpdateRequest request = new UpdateRequest()
                .withSmartAdTargets(
                        LongStreamEx.range(1L, 1_002L)
                                .boxed()
                                .map(id -> new SmartAdTargetUpdateItem()
                                        .withId(id))
                                .toList()
                );
        genericApiService.doAction(delegate, request);
    }

    @Test
    public void update_changePropertiesWhenCpcStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
        PerformanceFilter startFilter = getActualFilter(adGroupId);
        String newName = "new filter name";
        long newCpc = startFilter.getPriceCpa().longValue() + 100L;

        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withName(newName)
                .withPriceCpc(BigDecimal.valueOf(newCpc))
                .withPriceCpa(initPriceCpa)
                .withTargetFunnel(TargetFunnel.SAME_PRODUCTS);

        UpdateRequest request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        PerformanceFilter actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void update_changeConditions_success() {
        steps.featureSteps()
                .addClientFeature(filterInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);

        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withConditions(otherFilterConditions())
                .withLastChange(null)
                .withTab(PerformanceFilterTab.CONDITION);

        UpdateRequest request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        PerformanceFilter actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void update_removeConditions_success() {
        steps.featureSteps()
                .addClientFeature(filterInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);

        var oldFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withConditions(otherFilterConditions())
                .withLastChange(null);

        var request = createRequest(filterId, oldFilter);
        sendRequest(request);

        var expectedFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withLastChange(null)
                .withConditions(null);

        request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        var actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter.withTab(PerformanceFilterTab.ALL_PRODUCTS));
    }

    @Test
    public void update_noChangesInConditions_noChangesInConditions() {
        steps.featureSteps()
                .addClientFeature(filterInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);

        var oldFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withConditions(otherFilterConditions())
                .withLastChange(null)
                .withTab(PerformanceFilterTab.CONDITION);

        var request = createRequest(filterId, oldFilter);
        sendRequest(request);

        var name = "Brobdingnag";
        request = new UpdateRequest()
                .withSmartAdTargets(
                        new SmartAdTargetUpdateItem()
                                .withId(filterId)
                                .withName(name)
                );
        sendRequest(request);

        var actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, oldFilter.withName(name));
    }

    @Test
    public void update_setAvailableCondition_success() {
        steps.featureSteps()
                .addClientFeature(filterInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);
        steps.performanceFilterSteps()
                .setPerformanceFilterProperty(filterInfo, PerformanceFilter.CONDITIONS, otherFilterConditions());

        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withConditions(defaultFilterConditions())
                .withLastChange(null)
                .withTab(PerformanceFilterTab.TREE);

        UpdateRequest request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        PerformanceFilter actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void update_changeConditions_siteFeed_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(emptyList());
        PerformanceFilterInfo perfFilterInfo = new PerformanceFilterInfo()
                .withFilter(filter)
                .withAdGroupInfo(adGroupInfo);

        steps.performanceFilterSteps().addPerformanceFilter(shard, filter);
        steps.performanceFilterSteps()
                .setPerformanceFilterProperty(perfFilterInfo, PerformanceFilter.CONDITIONS,
                        List.of(defaultSiteFilterCondition().withParsedValue(true)));

        PerformanceFilterCondition condition =
                new PerformanceFilterCondition<>("price", Operator.RANGE, "[\"3000.00-100000.00\",\"111.00-222.00\"]")
                        .withParsedValue(Stream.of(
                                new DecimalRange("3000.00-100000.00"),
                                new DecimalRange("111.00-222.00")
                        ).collect(toList()));
        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withSource(Source.SITE)
                .withConditions(List.of(condition))
                .withLastChange(null);

        UpdateRequest request = createRequest(perfFilterInfo.getFilterId(), expectedFilter);
        sendRequest(request);

        List<PerformanceFilterCondition> actualConditions =
                getActualFilter(adGroupInfo.getAdGroupId()).getConditions();
        List<PerformanceFilterCondition> expectedConditions = List.of(
                defaultSiteFilterCondition().withParsedValue(null), condition);
        assertThat(actualConditions).isEqualTo(expectedConditions);
    }

    @Test
    public void update_setCategoryIdEqualsCondition_success() {
        steps.featureSteps()
                .addClientFeature(filterInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);
        steps.performanceFilterSteps()
                .setPerformanceFilterProperty(filterInfo, PerformanceFilter.CONDITIONS, otherFilterConditions());

        PerformanceFilterCondition<List<Long>> categoryCondition =
                new PerformanceFilterCondition<>("categoryId", Operator.EQUALS, "[\"123\",\"456\"]");
        categoryCondition.setParsedValue(List.of(123L, 456L));
        PerformanceFilter expectedFilter = defaultPerformanceFilter(adGroupId, filterInfo.getFeedId())
                .withConditions(singletonList(categoryCondition))
                .withTab(PerformanceFilterTab.TREE)
                .withLastChange(null);

        UpdateRequest request = createRequest(filterId, expectedFilter);
        sendRequest(request);

        PerformanceFilter actualFilter = getActualFilter(adGroupId);
        compareFilters(actualFilter, expectedFilter);
    }

    private void sendRequest(UpdateRequest request) {
        UpdateResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        if (!errors.isEmpty()) {
            String message = StreamEx.of(errors)
                    .map(e -> String.format("(code=%s, message=%s, details=%s)", e.getCode(), e.getMessage(),
                            e.getDetails()))
                    .joining(", ", "Unexpected errors: [", "]");
            throw new IllegalStateException(message);
        }
    }

}
