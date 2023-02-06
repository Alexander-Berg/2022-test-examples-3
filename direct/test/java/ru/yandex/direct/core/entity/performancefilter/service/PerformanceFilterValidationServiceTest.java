package ru.yandex.direct.core.entity.performancefilter.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessChecker;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessValidator;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.core.validation.defects.MoneyDefects;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterValidationService.MAX_FILTERS_COUNT;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.error;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class PerformanceFilterValidationServiceTest {
    private static final long CID = 111L;
    private static final long FEED_ID = 222L;
    private static final long AD_GROUP_ID = 333L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(444L);
    private static final long FILTER_ID = 555L;
    private static final long OPERATOR_UID = 777L;

    private AdGroup adGroup = defaultPerformanceAdGroup(CID, FEED_ID).withId(AD_GROUP_ID);
    private DbStrategy strategy =
            (DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
    private Campaign campaign = new Campaign().withId(CID).withStrategy(strategy);
    private FilterSchema filterSchema = new PerformanceDefault();
    private PerformanceFilter filter =
            TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID).withId(FILTER_ID);
    private PerformanceFilter oldFilter =
            TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID).withId(FILTER_ID);
    private PerformanceFilterValidationService performanceFilterValidationService;
    private Map<Long, List<PerformanceFilter>> filtersByAdGroupId = new HashMap<>();
    private PerformanceFilterRepository performanceFilterRepository;
    @Mock
    private List<PerformanceFilter> filtersWithSameAdGroupId;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(any(ClientId.class))).thenReturn(1);
        ClientService clientService = mock(ClientService.class);
        when(clientService.getWorkCurrency(any(ClientId.class))).thenReturn(CurrencyRub.getInstance());
        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);
        when(adGroupRepository.getAdGroupSimple(anyInt(), any(ClientId.class), anyCollection()))
                .thenReturn(singletonMap(AD_GROUP_ID, adGroup));
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getCampaigns(anyInt(), anyCollection())).thenReturn(singletonList(campaign));
        PerformanceFilterStorage filterSchemaServiceStorage = mock(PerformanceFilterStorage.class);
        when(filterSchemaServiceStorage.getFilterSchema(any(PerformanceFilter.class))).thenReturn(filterSchema);
        performanceFilterRepository = mock(PerformanceFilterRepository.class);
        when(performanceFilterRepository.getNotDeletedFiltersByAdGroupIds(anyInt(), anyCollection()))
                .thenReturn(filtersByAdGroupId);
        filtersByAdGroupId.put(AD_GROUP_ID, singletonList(filter));
        when(performanceFilterRepository.getFiltersById(anyInt(), anyCollection()))
                .thenReturn(singletonList(oldFilter));

        CampaignSubObjectAccessValidator accessValidator = mock(CampaignSubObjectAccessValidator.class);
        when(accessValidator.apply(anyLong())).thenAnswer(args -> ValidationResult.success(args.getArgument(0)));
        CampaignSubObjectAccessChecker accessChecker = mock(CampaignSubObjectAccessChecker.class);
        when(accessChecker.createValidator(any(), any())).thenReturn(accessValidator);
        CampaignSubObjectAccessCheckerFactory accessCheckerFactory = mock(CampaignSubObjectAccessCheckerFactory.class);
        when(accessCheckerFactory.newAdGroupChecker(anyLong(), eq(CLIENT_ID), anyCollection())).thenReturn(accessChecker);


        performanceFilterValidationService = new PerformanceFilterValidationService(shardHelper,
                clientService,
                adGroupRepository,
                campaignRepository,
                performanceFilterRepository,
                filterSchemaServiceStorage,
                accessCheckerFactory);
    }

    @Test
    public void validate_success() {
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_failure_whenWrongAdGroupId() {
        filter.withPid(AD_GROUP_ID + 1L);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(AdGroupDefects.notFound()));
    }

    @Test
    public void validate_failure_whenAdGroupNotPerformance() {
        adGroup.withType(AdGroupType.BASE);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(AdGroupDefects.inconsistentAdGroupType()));
    }

    @Test
    public void validate_failure_whenConditionsIsEmpty() {
        filter.withConditions(emptyList());
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CollectionDefects.notEmptyCollection()));
    }

    @Test
    public void validate_failure_whenConditionsIsTooLong() {
        List<String> strRanges = IntStreamEx.range(1, 65536, 10)
                .boxed()
                .map(i -> String.format("%1$d-%2$d", i, i + 9))
                .toList();
        List<DecimalRange> ranges = mapList(strRanges, DecimalRange::new);
        String stringValue = StreamEx.of(strRanges)
                .joining("\",\"", "[\"", "\"]");
        PerformanceFilterCondition<List<DecimalRange>> condition =
                new PerformanceFilterCondition<>("oldprice", Operator.RANGE, stringValue);
        condition.setParsedValue(ranges);
        filter.getConditions().add(condition);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(PerformanceFilterDefects.filterConditionsIsTooLong()));
    }

    @Test
    public void validate_failure_whenConditionsIsTooMany() {
        List<PerformanceFilterCondition<Boolean>> conditions = IntStreamEx.range(1, 50)
                .boxed()
                .map(i -> String.format("field%1$d", i))
                .map(fieldName -> new PerformanceFilterCondition<Boolean>(fieldName, Operator.EQUALS, "true"))
                .peek(condition -> condition.setParsedValue(Boolean.TRUE))
                .toList();
        filter.getConditions().addAll(conditions);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CollectionDefects.collectionSizeIsValid(0, 30)));
    }

    @Test
    public void validate_failure_whenFieldNotInFilterSchema() {
        PerformanceFilterCondition<Boolean> condition =
                new PerformanceFilterCondition<>("йцукен", Operator.EQUALS, "false");
        condition.setParsedValue(Boolean.FALSE);
        filter.getConditions().add(condition);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(PerformanceFilterDefects.unknownField()));
    }

    @Test
    public void validate_failure_whenInConditionsUnknownOperator() {
        PerformanceFilterCondition<Boolean> condition =
                new PerformanceFilterCondition<>("available", Operator.UNKNOWN, "true");
        condition.setParsedValue(Boolean.TRUE);
        filter.getConditions().add(condition);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(PerformanceFilterDefects.unknownOperator()));
    }

    @Test
    public void validate_failure_whenInConditionsOnlyAvailable() {
        PerformanceFilterCondition<Boolean> condition =
                new PerformanceFilterCondition<>("available", Operator.EQUALS, "true");
        condition.setParsedValue(Boolean.TRUE);
        filter.setConditions(singletonList(condition));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(PerformanceFilterDefects.mustContainAnyMoreConditions()));
    }

    @Test
    public void validate_failure_whenConditionHasEmptyValue() {
        PerformanceFilterCondition<List<Long>> condition =
                new PerformanceFilterCondition<>("categoryId", Operator.EQUALS, "[\"\"]");
        condition.setParsedValue(emptyList());
        filter.getConditions().add(condition);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CollectionDefects.notEmptyCollection()));
    }

    @Test
    public void validate_failure_whenNameIsNull() {
        filter.withName(null);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CommonDefects.notNull()));
    }

    @Test
    public void validate_failure_whenNameCharsNotAllowed() {
        filter.withName("<>");
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(BannerDefects.restrictedCharsInField()));
    }

    @Test
    public void validate_failure_whenStringValueNotAllowed() {
        String emojiSymbol = "\uD83D\uDEA9";
        PerformanceFilterCondition<Boolean> condition =
                new PerformanceFilterCondition<>("description", Operator.CONTAINS, emojiSymbol);
        condition.setParsedValue(Boolean.FALSE);
        filter.setConditions(singletonList(condition));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(BannerDefects.restrictedCharsInField()));
    }

    @Test
    public void validate_failure_whenNameIsTooLong() {
        filter.withName(StringUtils.repeat('A', 101));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CollectionDefects.maxStringLength(100)));
    }

    @Test
    public void validate_failure_whenNameIsBlank() {
        filter.withName("   ");
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(StringDefects.notEmptyString()));
    }

    @Test
    public void validate_failure_whenTargetFunnelIsNull() {
        filter.withTargetFunnel(null);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CommonDefects.notNull()));
    }

    @Test
    public void validate_success_whenCpcIsNull() {
        filter.withPriceCpc(null);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_success_whenCpcIsZero() {
        filter.withPriceCpc(BigDecimal.valueOf(0.0d));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_success_whenCpaIsNull() {
        filter.withPriceCpa(null);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_success_whenCpaIsZero() {
        filter.withPriceCpa(BigDecimal.valueOf(0.0d));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_failure_whenCpcIsTooLow() {
        filter.withPriceCpc(BigDecimal.valueOf(0.01));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(MoneyDefects.invalidValueNotLessThan(Money.valueOf(1, CurrencyCode.RUB))));
    }

    @Test
    public void validate_failure_whenCpcIsTooHigh() {
        filter.withPriceCpc(BigDecimal.valueOf(100000));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(MoneyDefects.invalidValueNotGreaterThan(Money.valueOf(25000.0, CurrencyCode.RUB))));
    }

    @Test
    public void validate_failure_whenCpaIsTooLow() {
        filter.withPriceCpa(BigDecimal.valueOf(0.01));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(MoneyDefects.invalidValueNotLessThan(Money.valueOf(1, CurrencyCode.RUB))));
    }

    @Test
    public void validate_failure_whenCpaIsTooHigh() {
        filter.withPriceCpa(BigDecimal.valueOf(100000));
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(MoneyDefects.invalidValueNotGreaterThan(Money.valueOf(25000.0, CurrencyCode.RUB))));
    }

    @Test
    public void validate_failure_whenAutobudgetPriorityNotSet() {
        campaign.withStrategy((DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_ROI));
        filter.withAutobudgetPriority(null);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CommonDefects.notNull()));
    }

    @Test
    public void validate_failure_whenAutobudgetPriorityIsWrong() {
        campaign.withStrategy((DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_ROI));
        filter.withAutobudgetPriority(100);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(CommonDefects.invalidValue()));
    }

    @Test
    public void validate_failure_whenFilterCountIsTooLarge() {
        //эмулируем валидацию нового фильтра, при уже максимальном количестве фильтров в группе
        filter.withId(null);
        filtersByAdGroupId.put(AD_GROUP_ID, filtersWithSameAdGroupId);
        when(filtersWithSameAdGroupId.size()).thenReturn(MAX_FILTERS_COUNT);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, singletonList(filter));
        assertThat(vr).has(error(PerformanceFilterDefects.filterCountIsTooLarge(MAX_FILTERS_COUNT)));
    }

    @Test
    public void validate_failure_whenFiltersContainDuplicatesToExistingFiltersWithSameAdGroupId() {
        PerformanceFilter duplicateFilter = TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID)
                .withId(FILTER_ID + 1L);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, asList(duplicateFilter));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(CollectionDefects.duplicatedObject().defectId()))));
    }

    @Test
    public void validate_failure_whenNewFiltersWithSameAdGroupIdContainDuplicates() {
        // нужно, чтобы ошибка про дубликаты не вернулась при сравнении новых и "существующего" фильтра,
        // а сработала только для новых фильтров
        when(performanceFilterRepository.getNotDeletedFiltersByAdGroupIds(anyInt(), anyCollection()))
                .thenReturn(Map.of());
        when(performanceFilterRepository.getFiltersById(anyInt(), anyCollection()))
                .thenReturn(emptyList());

        PerformanceFilter firstFilter = TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID);
        PerformanceFilter secondFilter = TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, asList(firstFilter, secondFilter));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(CollectionDefects.duplicatedObject().defectId()))));
    }

    @Test
    public void validate_success_ignoreDuplicatesWhenDeletingFilters() {
        // создаем ситуацию, когда в базе уже есть дубликаты, и нам нужно успешно удалить один из них
        PerformanceFilter duplicateFilter =
                TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID).withId(FILTER_ID + 1);
        filtersByAdGroupId.put(AD_GROUP_ID, List.of(oldFilter, duplicateFilter));
        when(performanceFilterRepository.getFiltersById(anyInt(), anyCollection()))
                .thenReturn(List.of(oldFilter, duplicateFilter));

        PerformanceFilter duplicateDeleteFilter = TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID)
                .withId(FILTER_ID + 1)
                .withIsDeleted(true);
        ValidationResult<List<PerformanceFilter>, Defect> vr =
                performanceFilterValidationService.validate(CLIENT_ID, OPERATOR_UID, asList(duplicateDeleteFilter));
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }
}
