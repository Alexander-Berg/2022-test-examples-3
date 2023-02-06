package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.AdTargetStateSelectionEnum;
import com.yandex.direct.api.v5.general.AdTargetsSelectionCriteria;
import com.yandex.direct.api.v5.general.ConditionTypeEnum;
import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StringConditionOperatorEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.smartadtargets.AudienceEnum;
import com.yandex.direct.api.v5.smartadtargets.ConditionsArray;
import com.yandex.direct.api.v5.smartadtargets.ConditionsItem;
import com.yandex.direct.api.v5.smartadtargets.GetRequest;
import com.yandex.direct.api.v5.smartadtargets.GetResponse;
import com.yandex.direct.api.v5.smartadtargets.SmartAdTargetFieldEnum;
import com.yandex.direct.api.v5.smartadtargets.SmartAdTargetGetItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.smartadtargets.converter.GetResponseConverterService;
import ru.yandex.direct.api.v5.entity.smartadtargets.validation.SmartAdTargetValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange;
import ru.yandex.direct.core.entity.performancefilter.model.NowOptimizingBy;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.FACTORY;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class GetSmartAdTargetDelegateTest {

    private static final Comparator<ConditionsItem> CONDITIONS_ITEM_COMPARATOR =
            Comparator.comparing(ConditionsItem::getOperand)
                    .thenComparing(ConditionsItem::getOperator)
                    .thenComparing(l -> String.join(";", l.getArguments()));

    @Autowired
    private Steps steps;
    @Autowired
    private SmartAdTargetValidationService validationService;
    @Autowired
    private PerformanceFilterService performanceFilterService;
    @Autowired
    private GetResponseConverterService responseConverterService;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    private GenericApiService genericApiService;
    private GetSmartAdTargetDelegate delegate;

    private Integer shard;
    private ClientInfo clientInfo;
    private PerformanceFilterInfo firstFilterInfo;

    public static GetRequest createGetRequest(Long filterId) {
        return new GetRequest()
                .withSelectionCriteria(
                        new AdTargetsSelectionCriteria()
                                .withIds(singleton(filterId)))
                .withFieldNames(SmartAdTargetFieldEnum.values());
    }

    private static void sortSmartAdTargetGetItem(SmartAdTargetGetItem item) {
        ConditionsArray conditionsArray = item.getConditions().getValue();
        List<ConditionsItem> items = conditionsArray.getItems();
        items.sort(CONDITIONS_ITEM_COMPARATOR);
        conditionsArray.setItems(items);
    }

    @Before
    public void before() {
        firstFilterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        shard = firstFilterInfo.getShard();
        clientInfo = firstFilterInfo.getClientInfo();
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
        delegate = new GetSmartAdTargetDelegate(auth, validationService, performanceFilterService,
                responseConverterService);
    }

    @Test
    public void get_checkResultFormat_success() {
        String name = "test feed #2";
        long cpc = 22L;
        long cpa = 33L;
        String availableOperand = "available";
        String priceOperand = "price";
        String priceRange1 = "3000-100000";
        String priceRange2 = "111-222";
        String categoryIdOperand = "categoryId";
        String categoryIdArgument = "3";
        String idOperand = "id";
        String idArgument = "10";
        String nameOperand = "name";
        String nameArgument = "test name";
        PerformanceFilter newPerfFilter = new PerformanceFilter()
                .withPid(firstFilterInfo.getAdGroupId())
                .withFeedId(firstFilterInfo.getFeedId())
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withName(name)
                .withNowOptimizingBy(NowOptimizingBy.CPC)
                .withPriceCpc(BigDecimal.valueOf(cpc))
                .withPriceCpa(BigDecimal.valueOf(cpa))
                .withStatusBsSynced(StatusBsSynced.NO)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withLastChange(LocalDate.now().atTime(0, 0))
                .withAutobudgetPriority(null)
                .withConditions(
                        List.of(
                                new PerformanceFilterCondition<Boolean>(availableOperand, Operator.EQUALS, "true")
                                        .withParsedValue(Boolean.TRUE),
                                new PerformanceFilterCondition<List<DecimalRange>>(priceOperand, Operator.RANGE,
                                        "[\"" + priceRange1 + "\",\"" + priceRange2 + "\"]")
                                        .withParsedValue(List.of(
                                                new DecimalRange(priceRange1),
                                                new DecimalRange(priceRange2))),
                                new PerformanceFilterCondition<Long>(categoryIdOperand, Operator.EQUALS,
                                        "[\"" + categoryIdArgument + "\"]")
                                        .withParsedValue(parseLong(categoryIdArgument)),
                                new PerformanceFilterCondition<Long>(idOperand, Operator.LESS,
                                        "[\"" + idArgument + "\"]")
                                        .withParsedValue(parseLong(idArgument)),
                                new PerformanceFilterCondition<String>(nameOperand, Operator.CONTAINS, nameArgument)
                                        .withParsedValue(nameArgument)
                        ))
                .withTab(PerformanceFilterTab.TREE);
        Long newFilterId =
                performanceFilterRepository.addPerformanceFilters(shard, singletonList(newPerfFilter)).get(0);

        SmartAdTargetGetItem expectedItem = new SmartAdTargetGetItem()
                .withId(newFilterId)
                .withAdGroupId(firstFilterInfo.getAdGroupId())
                .withName(name)
                .withAverageCpc(FACTORY.createSmartAdTargetGetItemAverageCpc(cpc * 1_000_000L))
                .withAverageCpa(FACTORY.createSmartAdTargetGetItemAverageCpa(cpa * 1_000_000L))
                .withAudience(AudienceEnum.VISITED_PRODUCT_PAGE)
                .withAvailableItemsOnly(FACTORY.createSmartAdTargetGetItemAvailableItemsOnly(YesNoEnum.YES))
                .withConditionType(ConditionTypeEnum.ITEMS_SUBSET)
                .withConditions(FACTORY.createSmartAdTargetGetItemConditions(
                        new ConditionsArray()
                                .withItems(
                                        new ConditionsItem()
                                                .withOperand(priceOperand)
                                                .withOperator(StringConditionOperatorEnum.IN_RANGE)
                                                .withArguments(priceRange1, priceRange2),
                                        new ConditionsItem()
                                                .withOperand(categoryIdOperand)
                                                .withOperator(StringConditionOperatorEnum.EQUALS_ANY)
                                                .withArguments(categoryIdArgument),
                                        new ConditionsItem()
                                                .withOperand(idOperand)
                                                .withOperator(StringConditionOperatorEnum.LESS_THAN)
                                                .withArguments(idArgument),
                                        new ConditionsItem()
                                                .withOperand(nameOperand)
                                                .withOperator(StringConditionOperatorEnum.CONTAINS_ANY)
                                                .withArguments(nameArgument)
                                )))
                .withCampaignId(firstFilterInfo.getCampaignId())
                .withState(StateEnum.OFF)
                .withStrategyPriority(FACTORY.createSmartAdTargetGetItemStrategyPriority(null));
        sortSmartAdTargetGetItem(expectedItem);

        GetRequest request = createGetRequest(newFilterId);
        GetResponse response = genericApiService.doAction(delegate, request);

        SmartAdTargetGetItem actualItem = response.getSmartAdTargets().get(0);
        sortSmartAdTargetGetItem(actualItem);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_byAdGroupId_success() {
        GetRequest request = new GetRequest()
                .withSelectionCriteria(
                        new AdTargetsSelectionCriteria()
                                .withAdGroupIds(singleton(firstFilterInfo.getAdGroupId())))
                .withFieldNames(SmartAdTargetFieldEnum.values());

        GetResponse response = genericApiService.doAction(delegate, request);

        List<Long> perfFiltersIds = mapList(response.getSmartAdTargets(), SmartAdTargetGetItem::getId);
        assertThat(perfFiltersIds).contains(firstFilterInfo.getFilterId());
    }

    @Test
    public void get_byCampaignId_success() {
        GetRequest request = new GetRequest()
                .withSelectionCriteria(
                        new AdTargetsSelectionCriteria()
                                .withCampaignIds(singleton(firstFilterInfo.getCampaignId())))
                .withFieldNames(SmartAdTargetFieldEnum.values());

        GetResponse response = genericApiService.doAction(delegate, request);

        List<Long> perfFiltersIds = mapList(response.getSmartAdTargets(), SmartAdTargetGetItem::getId);
        assertThat(perfFiltersIds).contains(firstFilterInfo.getFilterId());
    }

    @Test
    public void get_whenNoConditions_success() {
        PerformanceFilter filter =
                defaultPerformanceFilter(firstFilterInfo.getAdGroupId(), firstFilterInfo.getFeedId())
                        .withConditions(emptyList())
                        .withTab(PerformanceFilterTab.ALL_PRODUCTS);
        PerformanceFilterInfo filterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(firstFilterInfo.getAdGroupInfo())
                .withFilter(filter);
        steps.performanceFilterSteps().addPerformanceFilter(filterInfo);

        GetRequest request = createGetRequest(filter.getPerfFilterId())
                .withFieldNames(SmartAdTargetFieldEnum.values());
        GetResponse response = genericApiService.doAction(delegate, request);
        checkState(response.getSmartAdTargets().size() == 1);

        JAXBElement<ConditionsArray> conditions = response.getSmartAdTargets().get(0).getConditions();
        assertThat(conditions.isNil()).as("Returned conditions is nil")
                .isTrue();
    }

    @Test
    public void get_withoutDefaultConditionWhenSiteFeed_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(List.of(defaultSiteFilterCondition().withParsedValue(true)));

        steps.performanceFilterSteps().addPerformanceFilter(new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(filter));

        GetRequest request = createGetRequest(filter.getPerfFilterId())
                .withFieldNames(SmartAdTargetFieldEnum.values());
        GetResponse response = genericApiService.doAction(delegate, request);
        checkState(response.getSmartAdTargets().size() == 1);

        JAXBElement<ConditionsArray> conditions = response.getSmartAdTargets().get(0).getConditions();
        assertThat(conditions.isNil()).as("Returned conditions is nil")
                .isTrue();
    }

    @Test
    public void get_withWrongStateByCampaignId_emptyResult() {
        steps.performanceFilterSteps().setPerformanceFilterProperty(firstFilterInfo, PerformanceFilter.IS_SUSPENDED,
                true);
        GetRequest request = new GetRequest()
                .withSelectionCriteria(
                        new AdTargetsSelectionCriteria()
                                .withCampaignIds(singleton(firstFilterInfo.getCampaignId()))
                                .withStates(AdTargetStateSelectionEnum.ON))
                .withFieldNames(SmartAdTargetFieldEnum.values());

        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getSmartAdTargets()).isEmpty();
    }

    @Test
    public void get_whenSomeoneElsePerfFilter_failure() {
        PerformanceFilterInfo someoneElsesFilterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        GetRequest request = createGetRequest(someoneElsesFilterInfo.getFilterId());
        GetResponse response = genericApiService.doAction(delegate, request);

        List<SmartAdTargetGetItem> smartAdTargets = response.getSmartAdTargets();
        assertThat(smartAdTargets).isEmpty();
    }

}
