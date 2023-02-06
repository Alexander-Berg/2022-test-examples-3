package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.SetBidsActionResult;
import com.yandex.direct.api.v5.smartadtargets.SetBidsItem;
import com.yandex.direct.api.v5.smartadtargets.SetBidsRequest;
import com.yandex.direct.api.v5.smartadtargets.SetBidsResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.smartadtargets.converter.RequestConverter;
import ru.yandex.direct.api.v5.entity.smartadtargets.validation.SetBidsValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.keyword.model.AutoBudgetPriority;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterSetBidsService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.FACTORY;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class SetBidsSmartAdTargetDelegateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private SetBidsValidationService validationService;
    @Autowired
    private PerformanceFilterSetBidsService performanceFilterSetBidsService;
    @Autowired
    private RequestConverter requestConverter;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private SetBidsSmartAdTargetDelegate delegate;
    private Integer shard;
    private PerformanceFilterInfo filterInfo1;
    private PerformanceAdGroupInfo adGroupInfo2;
    private CampaignInfo campaignInfo3;
    private Long perfFilterId2;

    @Before
    public void before() {
        filterInfo1 = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        shard = filterInfo1.getShard();
        ClientInfo clientInfo = filterInfo1.getClientInfo();

        adGroupInfo2 = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo2.getAdGroupId(), adGroupInfo2.getFeedId())
                .withConditions(otherFilterConditions());
        perfFilterId2 = steps.performanceFilterSteps().addPerformanceFilter(shard, filter);

        campaignInfo3 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);


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
        delegate = new SetBidsSmartAdTargetDelegate(auth, validationService, performanceFilterSetBidsService,
                requestConverter, resultConverter, ppcPropertiesSupport, featureService);
    }

    @Test
    public void test_byCampaignIdWhenCpaStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpa(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withCampaignId(filterInfo1.getCampaignId())
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_byAdgroupIdWhenCpaStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpa(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withAdGroupId(filterInfo1.getAdGroupId())
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_byPerfFilterIdWhenCpaStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpa(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_whenCpaStrategy_successButIgnoreCpcAndPriorityValues() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpa(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L))
                        .withStrategyPriority(PriorityEnum.HIGH));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("Has warnings").hasSize(2);
    }

    @Test
    public void test_whenRoiStrategy_successButIgnorePriceValues() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), StrategyName.AUTOBUDGET_ROI);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withAutobudgetPriority(AutoBudgetPriority.HIGH.getTypedValue());

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L))
                        .withStrategyPriority(PriorityEnum.HIGH));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("Has warnings").hasSize(2);
    }

    @Test(expected = ApiValidationException.class)
    public void test_whenSetCampaignIdAndAdGroupId_failure() {
        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withCampaignId(filterInfo1.getCampaignId())
                        .withAdGroupId(filterInfo1.getAdGroupId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        genericApiService.doAction(delegate, request);
    }

    @Test(expected = ApiValidationException.class)
    public void test_whenSetAdGroupIdAndPerfFilterId_failure() {
        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withAdGroupId(filterInfo1.getAdGroupId())
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        genericApiService.doAction(delegate, request);
    }

    @Test
    public void test_whenNotExistFiltersInCampaign_warning() {
        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withCampaignId(campaignInfo3.getCampaignId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("Has warning").hasSize(1);
    }

    @Test
    public void test_whenDataNotValid_failure() {
        PerformanceFilter startFilter = performanceFilterRepository.getFiltersById(shard, singleton(perfFilterId2))
                .get(0);

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withCampaignId(adGroupInfo2.getCampaignId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(999999_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(888888_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);
        List<ExceptionNotification> errors = setBidsActionResult.getErrors();

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(perfFilterId2))
                        .get(0);
        compareFilters(actualFilter, startFilter);
        assertThat(errors).as("Has errors").hasSize(2);
    }

    @Test
    public void test_byCampaignIdWhenCpcStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpc(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withCampaignId(filterInfo1.getCampaignId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_byAdgroupIdWhenCpcStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpc(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withAdGroupId(filterInfo1.getAdGroupId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_byPerfFilterIdWhenCpcStrategy_success() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpc(BigDecimal.valueOf(222L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(222_000_000L)));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("No warnings").isEmpty();
    }

    @Test
    public void test_whenCpcStrategy_successButIgnoreCpaAndPriorityValues() {
        steps.campaignSteps().setStrategy(filterInfo1.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);
        PerformanceFilter expectedFilter = defaultPerformanceFilter(filterInfo1.getAdGroupId(), filterInfo1.getFeedId())
                .withLastChange(null)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriceCpc(BigDecimal.valueOf(111L));

        List<SetBidsItem> items = List.of(
                new SetBidsItem()
                        .withId(filterInfo1.getFilterId())
                        .withAverageCpc(FACTORY.createSmartAdTargetUpdateItemAverageCpc(111_000_000L))
                        .withAverageCpa(FACTORY.createSmartAdTargetUpdateItemAverageCpa(222_000_000L))
                        .withStrategyPriority(PriorityEnum.HIGH));
        SetBidsRequest request = new SetBidsRequest()
                .withBids(items);
        SetBidsResponse setBidsResponse = genericApiService.doAction(delegate, request);
        SetBidsActionResult setBidsActionResult = setBidsResponse.getSetBidsResults().get(0);

        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singleton(filterInfo1.getFilterId()))
                        .get(0);
        compareFilters(actualFilter, expectedFilter);
        assertThat(setBidsActionResult.getErrors()).as("No errors").isEmpty();
        assertThat(setBidsActionResult.getWarnings()).as("Has warnings").hasSize(2);
    }
}
