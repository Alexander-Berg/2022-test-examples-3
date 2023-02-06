package ru.yandex.direct.grid.processing.service.smartfilter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.util.RawValue;
import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.keyword.model.AutoBudgetPriority;
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaPerCampStrategy;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTab;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdUpdateSmartFilterCondition;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdUpdateSmartFilters;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdUpdateSmartFiltersItem;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdUpdateSmartFiltersPayload;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdUpdateSmartFiltersPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP;
import static ru.yandex.direct.core.entity.performancefilter.utils.PerformanceFilterUtils.PERFORMANCE_FILTER_CONDITION_COMPARATOR;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.smartfilter.SmartFilterConverter.toGdAutobudgetPriority;
import static ru.yandex.direct.grid.processing.service.smartfilter.SmartFilterConverter.toGdTargetFunnel;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.NumberUtils.greaterThanZero;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmartFilterGraphQlServiceUpdateTest {

    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedItems {\n"
            + "         id,\n"
            + "     }\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "      warnings{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";
    private static final String UPDATE_MUTATION_NAME = "updateFilters";
    private static final TargetFunnel NEW_TARGET_FUNNEL = TargetFunnel.PRODUCT_PAGE_VISIT;
    private static final Integer MEDIUM_PRIORITY = AutoBudgetPriority.MEDIUM.getTypedValue();
    private static final Integer LOW_PRIORITY = AutoBudgetPriority.LOW.getTypedValue();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private Long adGroupId;
    private Integer shard;
    private Long filterId;
    private Long feedId;
    private GridGraphQLContext context;
    private PerformanceFilterInfo filterInfo;
    private ClientInfo clientInfo;
    private PerformanceFilter beforeFilter;

    @Before
    public void before() {
        filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        feedId = filterInfo.getFeedId();
        filterId = filterInfo.getFilterId();
        adGroupId = filterInfo.getAdGroupId();
        clientInfo = filterInfo.getClientInfo();
        beforeFilter = filterInfo.getFilter();
        shard = filterInfo.getShard();

        Long uid = filterInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(shard, singletonList(uid)).get(0);
        TestAuthHelper.setDirectAuthentication(user);
        context = buildContext(user);
    }

    @Test
    public void updateFilter_success_whenChangedTargetFunnel() {
        // Подготавливаем и проверяем исходные данные
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withTargetFunnel(NEW_TARGET_FUNNEL);
        assumeThat(beforeFilter.getTargetFunnel(), not(NEW_TARGET_FUNNEL));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        TargetFunnel actualTargetFunnel = actualFilter.getTargetFunnel();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualTargetFunnel).isEqualTo(changedFilter.getTargetFunnel());
        });
    }

    @Test
    public void updateFilter_success_whenChangedConditionsRenew() {
        // Подготавливаем исходные данные
        List<PerformanceFilterCondition> changedFilterConditions = otherFilterConditions();
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withConditions(changedFilterConditions);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long newFilterId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter newActualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(newFilterId)).get(0);
        List<PerformanceFilterCondition> actualConditions = newActualFilter.getConditions();
        actualConditions.sort(PERFORMANCE_FILTER_CONDITION_COMPARATOR);
        checkState(actualConditions.size() == changedFilterConditions.size());
        PerformanceFilter oldActualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(newFilterId).isNotEqualTo(filterId);
            soft.assertThat(oldActualFilter.getIsDeleted()).isTrue();
            for (int i = 0; i < actualConditions.size(); i++) {
                soft.assertThat(actualConditions.get(i))
                        .is(matchedBy(beanDiffer(changedFilterConditions.get(i))
                                .useCompareStrategy(onlyExpectedFields())));
            }
        });
    }

    @Test
    public void updateFilter_success_whenChangedConditionsUpdateByFeature() {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), FeatureName.UPDATE_FILTER_CONDITIONS_ALLOWED, true);

        // Подготавливаем исходные данные
        List<PerformanceFilterCondition> changedFilterConditions = otherFilterConditions();
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withConditions(changedFilterConditions);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long newFilterId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        List<PerformanceFilterCondition> actualConditions = actualFilter.getConditions();
        actualConditions.sort(PERFORMANCE_FILTER_CONDITION_COMPARATOR);
        checkState(actualConditions.size() == changedFilterConditions.size());
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(newFilterId).isEqualTo(filterId);
            soft.assertThat(actualFilter.getIsDeleted()).isFalse();
            for (int i = 0; i < actualConditions.size(); i++) {
                soft.assertThat(actualConditions.get(i))
                        .is(matchedBy(beanDiffer(changedFilterConditions.get(i))
                                .useCompareStrategy(onlyExpectedFields())));
            }
        });
    }

    @Test
    public void updateFilter_success_whenSrcPriceCpaAndCpcIsZero() {
        // Подготавливаем исходные данные
        assumeThat(beforeFilter.getPriceCpc(), not(comparesEqualTo(BigDecimal.ZERO)));
        assumeThat(beforeFilter.getPriceCpa(), not(comparesEqualTo(BigDecimal.ZERO)));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(BigDecimal.ZERO)
                .withPriceCpc(BigDecimal.ZERO);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualFilter.getPriceCpa()).isEqualByComparingTo(BigDecimal.ZERO);
            soft.assertThat(actualFilter.getPriceCpc()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    public void updateFilter_failure_whenStringValueHasNoValues() {
        // Подготавливаем исходные данные
        PerformanceFilterCondition<List<Double>> condition =
                new PerformanceFilterCondition<>("categoryId", Operator.EQUALS, "[\"\"]");
        condition.setParsedValue(emptyList());
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpc(null);
        changedFilter.withConditions(singletonList(condition));

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("DefectIds.CANNOT_BE_NULL")
                                .withPath("updateItems[0].conditions[0].stringValue")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilter_failure_whenCpcIsTooSmall() {
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);

        // Подготавливаем исходные данные
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpc(BigDecimal.valueOf(0.01d))
                .withPriceCpa(null);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN")
                                .withParams(map("moneyValue", map(
                                        "currencyCode", new RawValue("RUB"),
                                        "zero", Boolean.FALSE,
                                        "inCurrencyRange", Boolean.TRUE),
                                        "moneyPriceValue", BigDecimal.valueOf(1L)))
                                .withPath("updateItems[0].priceCpc")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload).isEqualTo(expectedPayload);
    }

    @Test
    public void updateFilter_failure_whenCpaIsTooSmall() {
        // Подготавливаем исходные данные
        assumeThat(filterInfo.getCampaignInfo().getCampaign().getStrategy(),
                instanceOf(AverageCpaPerCampStrategy.class));
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpa(BigDecimal.valueOf(0.01d))
                .withPriceCpc(null);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN")
                                .withParams(map("moneyValue", map(
                                        "currencyCode", new RawValue("RUB"),
                                        "zero", Boolean.FALSE,
                                        "inCurrencyRange", Boolean.TRUE),
                                        "moneyPriceValue", new BigDecimal(1)))
                                .withPath("updateItems[0].priceCpa")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload).isEqualTo(expectedPayload);
    }

    @Test
    public void updateFilter_failure_whenNewNameIsTooLong() {
        // Подготавливаем исходные данные
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpc(null)
                .withName(StringUtils.repeat('A', 101));

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX")
                                .withParams(map("maxLength", 100))
                                .withPath("updateItems[0].name")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilter_failure_whenParsedValueIsWrong() {
        // Подготавливаем исходные данные
        PerformanceFilterCondition<List<DecimalRange>> condition =
                new PerformanceFilterCondition<>("oldprice", Operator.RANGE, "[\"20-10\"]");
        condition.setParsedValue(Stream.of(
                new DecimalRange("20-10"))
                .collect(toList()));
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpc(null);
        changedFilter.getConditions().add(condition);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("DefectIds.INVALID_VALUE")
                                .withPath("updateItems[0].conditions[2].stringValue")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilter_failure_onDuplicateFieldOperator() {
        // Подготавливаем исходные данные
        PerformanceFilterCondition<List<Long>> condition =
                new PerformanceFilterCondition<>("price", Operator.EQUALS, "[\"10\"]");
        condition.setParsedValue(singletonList(10L));
        PerformanceFilterCondition<List<Long>> conditionDuplicate =
                new PerformanceFilterCondition<>("price", Operator.EQUALS, "[\"20\"]");
        conditionDuplicate.setParsedValue(singletonList(20L));

        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withPriceCpc(null);
        changedFilter.setConditions(asList(condition, conditionDuplicate));

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS")
                                .withPath("updateItems[0].conditions")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilter_success_whenChangedTab() {
        //Проверяем исходное состояние и подготавливаем данные
        PerformanceFilterTab startTab =
                performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0).getTab();
        PerformanceFilterTab newTabValue = PerformanceFilterTab.ALL_PRODUCTS;
        assumeThat(startTab, not(newTabValue));
        assumeThat(startTab, notNullValue());
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withTab(newTabValue);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilterTab actualTab = performanceFilterRepository.getFiltersById(shard, singleton(filterId))
                .get(0).getTab();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualTab).isEqualTo(newTabValue);
        });
    }

    @Test
    public void updateFilter_success_whenTabChangedFromNull() {
        //Проверяем исходное состояние и подготавливаем данные
        PerformanceFilter filterWithoutTab = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withTab(null);
        performanceFilterRepository.addPerformanceFilters(shard, singletonList(filterWithoutTab));
        filterId = filterWithoutTab.getId();
        PerformanceFilterTab newTabValue = PerformanceFilterTab.ALL_PRODUCTS;
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withTab(newTabValue);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilterTab actualTab =
                performanceFilterRepository.getFiltersById(shard, singleton(filterId))
                        .get(0).getTab();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualTab).isEqualTo(newTabValue);
        });
    }

    @Test
    public void updateFilter_success_whenTabNotSet() {
        // Подготавливаем и проверяем исходные данные
        PerformanceFilterTab tabValue = PerformanceFilterTab.TREE;
        assumeThat(beforeFilter.getTab(), equalTo(tabValue));
        PerformanceFilter changedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withName("New test name")
                .withTab(null);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilterTab actualTab =
                performanceFilterRepository.getFiltersById(shard, singleton(filterId))
                        .get(0).getTab();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualTab).isEqualTo(tabValue);
        });
    }

    @Test
    public void updateFilter_success_keepCpcWhenChangingCpa() {
        // Подготавливаем и проверяем исходные данные
        assumeThat(filterInfo.getCampaignInfo().getCampaign().getStrategy(),
                instanceOf(AverageCpaPerCampStrategy.class));
        BigDecimal startPriceCpc = beforeFilter.getPriceCpc();
        assumeThat(greaterThanZero(startPriceCpc), is(true));
        BigDecimal newPriceCpa = BigDecimal.valueOf(25L);
        assumeThat(beforeFilter.getPriceCpa(), not(comparesEqualTo(newPriceCpa)));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(newPriceCpa);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId))
                .get(0);
        assumeThat(returnedId, equalTo(filterId));
        assumeThat(actualFilter.getPriceCpa(), comparesEqualTo(newPriceCpa));
        assertThat(actualFilter.getPriceCpc()).as("priceCpc").isEqualByComparingTo(startPriceCpc);
    }

    @Test
    public void updateFilter_success_keepCpaWhenChangingCpc() {
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), AUTOBUDGET_AVG_CPC_PER_CAMP);

        // Подготавливаем и проверяем исходные данные
        assumeThat(filterInfo.getCampaignInfo().getCampaign().getStrategy(),
                instanceOf(AverageCpaPerCampStrategy.class));
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        BigDecimal newPriceCpc = BigDecimal.valueOf(25L);
        assumeThat(beforeFilter.getPriceCpc(), not(comparesEqualTo(newPriceCpc)));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpc(newPriceCpc);

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        Long returnedId = payload.getUpdatedItems().get(0).getId();
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        assumeThat(returnedId, equalTo(filterId));
        assumeThat(actualFilter.getPriceCpc(), comparesEqualTo(newPriceCpc));
        assertThat(actualFilter.getPriceCpa()).as("priceCpa").isEqualByComparingTo(startPriceCpa);
    }

    @Test
    public void updateFilter_failure_WhenCpaStrategyAndTryToSetAutobudgetPriority() {
        // Подготавливаем и проверяем исходные данные
        assumeThat(filterInfo.getCampaignInfo().getCampaign().getStrategy(),
                instanceOf(AverageCpaPerCampStrategy.class));
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo, PerformanceFilter.AUTOBUDGET_PRIORITY,
                LOW_PRIORITY);
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withAutobudgetPriority(MEDIUM_PRIORITY);
        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(
                        new GdUpdateSmartFiltersPayloadItem().withId(filterId)))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list())
                        .withWarnings(list(new GdDefect()
                                .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                        ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                .withPath("updateItems[0].autobudgetPriority"))));
        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);
        //Проверяем результат
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload)
                    .as("Payload contains strategy warning")
                    .is(matchedBy(beanDiffer(expectedPayload)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFilter.getAutobudgetPriority())
                    .as("AutobudgetPriority wasn't changed")
                    .isEqualTo(LOW_PRIORITY);
        });
    }

    @Test
    public void updateFilter_failure_WhenCpaStrategyAndTryToSetCpc() {
        // Подготавливаем и проверяем исходные данные
        assumeThat(filterInfo.getCampaignInfo().getCampaign().getStrategy(),
                instanceOf(AverageCpaPerCampStrategy.class));
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        BigDecimal initPriceCpc = BigDecimal.valueOf(300L);
        BigDecimal newPriceCpc = BigDecimal.valueOf(400L);
        steps.performanceFilterSteps()
                .setPerformanceFilterProperty(filterInfo, PerformanceFilter.PRICE_CPC, initPriceCpc);
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpc(newPriceCpc);
        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(
                        new GdUpdateSmartFiltersPayloadItem().withId(filterId)))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list())
                        .withWarnings(list(new GdDefect()
                                .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                        ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                .withPath("updateItems[0].priceCpc"))));
        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);
        //Проверяем результат
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload)
                    .as("Payload contains strategy warning")
                    .is(matchedBy(beanDiffer(expectedPayload)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFilter.getPriceCpc())
                    .as("cpc price wasn't changed")
                    .isEqualByComparingTo(initPriceCpc);
        });
    }

    @Test
    public void updateFilter_warning_WhenCpcStrategyAndTryToSetCpaPrice() {
        // Подготавливаем и проверяем исходные данные
        steps.campaignSteps().setStrategy(filterInfo.getCampaignInfo(), StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
        BigDecimal newPriceCpa = BigDecimal.valueOf(55.55d);
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        assumeThat(startPriceCpa, not(comparesEqualTo(newPriceCpa)));
        BigDecimal newPriceCpc = BigDecimal.valueOf(66.66d);
        BigDecimal startPriceCpc = beforeFilter.getPriceCpc();
        assumeThat(greaterThanZero(startPriceCpc), is(true));
        assumeThat(startPriceCpc, not(comparesEqualTo(newPriceCpc)));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(newPriceCpa)
                .withPriceCpc(newPriceCpc);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(
                        new GdUpdateSmartFiltersPayloadItem().withId(filterId)))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list())
                        .withWarnings(list(new GdDefect()
                                .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                        ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                .withPath("updateItems[0].priceCpa"))));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload)
                    .as("Payload contains strategy warning")
                    .is(matchedBy(beanDiffer(expectedPayload)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFilter.getPriceCpc())
                    .as("PriceCpc was changed")
                    .isEqualByComparingTo(newPriceCpc);
            soft.assertThat(actualFilter.getPriceCpa())
                    .as("PriceCpa wasn't changed")
                    .isEqualByComparingTo(startPriceCpa);
        });
    }

    /**
     * Покрывает кейс найденной в продакшене ошибки: https://st.yandex-team.ru/DIRECT-98055
     */
    @Test
    public void updatePerformanceFilters_warningsButNoException_whenRoiAndNewPricesAreTheSameTheOldOnes() {
        // Подготавливаем и проверяем исходные данные
        setRoiStrategy(filterInfo.getCampaignInfo().getCampaignId());
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        BigDecimal startPriceCpc = beforeFilter.getPriceCpc();
        assumeThat(greaterThanZero(startPriceCpc), is(true));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(startPriceCpa)
                .withPriceCpc(startPriceCpc)
                .withAutobudgetPriority(LOW_PRIORITY);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(
                        new GdUpdateSmartFiltersPayloadItem().withId(filterId)))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list())
                        .withWarnings(list(new GdDefect()
                                        .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                                ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                        .withPath("updateItems[0].priceCpa"),
                                new GdDefect()
                                        .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                                ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                        .withPath("updateItems[0].priceCpc"))));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload)
                    .as("Payload contains strategy warning")
                    .is(matchedBy(beanDiffer(expectedPayload)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFilter.getPriceCpc())
                    .as("PriceCpc wasn't changed")
                    .isEqualByComparingTo(startPriceCpc);
            soft.assertThat(actualFilter.getPriceCpa())
                    .as("PriceCpa wasn't changed")
                    .isEqualByComparingTo(startPriceCpa);
        });
    }

    /**
     * Покрывает кейс найденной в продакшене ошибки: https://st.yandex-team.ru/DIRECT-98055
     */
    @Test
    public void updatePerformanceFilters_warningAndNotChangePricesInRoiCampaign() {
        // Подготавливаем и проверяем исходные данные
        setRoiStrategy(filterInfo.getCampaignInfo().getCampaignId());
        BigDecimal newPriceCpa = BigDecimal.valueOf(55.55d);
        BigDecimal startPriceCpa = beforeFilter.getPriceCpa();
        assumeThat(greaterThanZero(startPriceCpa), is(true));
        assumeThat(startPriceCpa, not(comparesEqualTo(newPriceCpa)));
        BigDecimal newPriceCpc = BigDecimal.valueOf(66.66d);
        BigDecimal startPriceCpc = beforeFilter.getPriceCpc();
        assumeThat(greaterThanZero(startPriceCpc), is(true));
        assumeThat(startPriceCpc, not(comparesEqualTo(newPriceCpc)));
        PerformanceFilter changedFilter = new PerformanceFilter()
                .withId(filterId)
                .withPriceCpa(newPriceCpa)
                .withPriceCpc(newPriceCpc)
                .withAutobudgetPriority(LOW_PRIORITY);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(singletonList(
                        new GdUpdateSmartFiltersPayloadItem().withId(filterId)))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list())
                        .withWarnings(list(new GdDefect()
                                        .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                                ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                        .withPath("updateItems[0].priceCpa"),
                                new GdDefect()
                                        .withCode("PerformanceFilterDefects.PerformanceFilterDefectIds" +
                                                ".INCONSISTENT_CAMPAIGN_STRATEGY")
                                        .withPath("updateItems[0].priceCpc"))));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(changedFilter);

        //Проверяем результат
        PerformanceFilter actualFilter = performanceFilterRepository.getFiltersById(shard, singleton(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload)
                    .as("Payload contains strategy warning")
                    .is(matchedBy(beanDiffer(expectedPayload)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualFilter.getPriceCpc())
                    .as("PriceCpc wasn't changed")
                    .isEqualByComparingTo(startPriceCpc);
            soft.assertThat(actualFilter.getPriceCpa())
                    .as("PriceCpa wasn't changed")
                    .isEqualByComparingTo(startPriceCpa);
        });
    }

    @Test
    public void updateFilter_failure_whenFilterDuplicatesOld() {
        // Подготавливаем исходные данные
        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT);
        Long secondFilterId = performanceFilterRepository.addPerformanceFilters(shard, singletonList(secondFilter))
                .get(0);
        PerformanceFilter secondChangedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(secondFilterId)
                .withName("new name 2")
                .withPriceCpc(null);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_OBJECTS")
                                .withPath("updateItems[0]")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = executeUpdate(secondChangedFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilters_failure_whenFiltersNotUnique() {
        // Подготавливаем исходные данные
        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT);
        Long secondFilterId = performanceFilterRepository.addPerformanceFilters(shard, singletonList(secondFilter))
                .get(0);
        PerformanceFilter firstChangedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(filterId)
                .withName("new name 1")
                .withPriceCpc(null);
        PerformanceFilter secondChangedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withId(secondFilterId)
                .withName("new name 2")
                .withPriceCpc(null);

        //Ожидаемый результат
        GdUpdateSmartFiltersPayload expectedPayload = new GdUpdateSmartFiltersPayload()
                .withUpdatedItems(list(null, null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                        .withCode("CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_OBJECTS")
                                        .withPath("updateItems[0]"),
                                new GdDefect()
                                        .withCode("CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_OBJECTS")
                                        .withPath("updateItems[1]")))
                        .withWarnings(list()));
        List<PerformanceFilter> filters = asList(firstChangedFilter, secondChangedFilter);

        //Выполняем запрос
        List<GdUpdateSmartFiltersItem> items = mapList(filters, this::toGdUpdateSmartFiltersItem);
        GdUpdateSmartFiltersPayload payload = sendRequest(items);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateFilter_success_massUpdate() {
        // Подготавливаем исходные данные
        // Проверяем, что фильтры принадлежащие разным кампанииям и с разными фидами корректно обработаются в одном
        // запросе.
        BigDecimal newCpa = BigDecimal.valueOf(444L);
        PerformanceAdGroupInfo secondAdGroupInfo =
                steps.adGroupSteps().addPerformanceAdGroup(new PerformanceAdGroupInfo().withClientInfo(clientInfo));
        PerformanceFilter secondFilter =
                defaultPerformanceFilter(secondAdGroupInfo.getAdGroupId(), secondAdGroupInfo.getFeedId())
                        .withConditions(otherFilterConditions());
        PerformanceFilterInfo secondFilterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(secondAdGroupInfo)
                .withFilter(secondFilter);
        steps.performanceFilterSteps().addPerformanceFilter(secondFilterInfo);
        List<PerformanceFilterInfo> filterInfos = list(filterInfo, secondFilterInfo);
        List<Long> filterIds = mapList(filterInfos, PerformanceFilterInfo::getFilterId);
        assumeThat(filterInfo.getFilter().getPriceCpa(), not(comparesEqualTo(newCpa)));
        assumeThat(secondFilterInfo.getFilter().getPriceCpa(), not(comparesEqualTo(newCpa)));
        //Изменённые фильтры
        List<GdUpdateSmartFiltersItem> gdUpdateSmartFiltersItems = StreamEx.of(filterInfo, secondFilterInfo)
                .map(fi -> new PerformanceFilter()
                        .withId(fi.getFilterId())
                        .withPriceCpa(newCpa))
                .map(this::toGdUpdateSmartFiltersItem)
                .toList();
        //Выполняем запрос
        GdUpdateSmartFiltersPayload payload = sendRequest(gdUpdateSmartFiltersItems);
        //Проверяем результат
        List<@GraphQLNonNull Long> returnedIds =
                mapList(payload.getUpdatedItems(), GdUpdateSmartFiltersPayloadItem::getId);
        List<PerformanceFilter> actualFilters = performanceFilterRepository.getFiltersById(shard, filterIds);
        Long[] ids = filterIds.toArray(new Long[0]);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedIds).containsExactlyInAnyOrder(ids);
            soft.assertThat(actualFilters.get(0).getPriceCpa()).isEqualByComparingTo(newCpa);
            soft.assertThat(actualFilters.get(1).getPriceCpa()).isEqualByComparingTo(newCpa);
        });
    }

    private void setRoiStrategy(Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget_roi)
                .set(CAMPAIGNS.STRATEGY_DATA, "{\"bid\": 15, \"sum\": 1500, \"name\": \"autobudget_roi\", " +
                        "\"goal_id\": \"43592887\", \"version\": 1, \"roi_coef\": 21, \"reserve_return\": 100}")
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    private GdUpdateSmartFiltersPayload executeUpdate(PerformanceFilter changedFilter) {
        GdUpdateSmartFiltersItem item = toGdUpdateSmartFiltersItem(changedFilter);
        List<@GraphQLNonNull GdUpdateSmartFiltersItem> updateItems = singletonList(item);
        return sendRequest(updateItems);
    }

    private GdUpdateSmartFiltersPayload sendRequest(List<@GraphQLNonNull GdUpdateSmartFiltersItem> updateItems) {
        GdUpdateSmartFilters request = new GdUpdateSmartFilters().withUpdateItems(updateItems);
        String query = String.format(UPDATE_MUTATION_TEMPLATE, UPDATE_MUTATION_NAME,
                GraphQlJsonUtils.graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        checkState(data.containsKey(UPDATE_MUTATION_NAME));
        return GraphQlJsonUtils.convertValue(data.get(UPDATE_MUTATION_NAME), GdUpdateSmartFiltersPayload.class);
    }

    private GdUpdateSmartFiltersItem toGdUpdateSmartFiltersItem(PerformanceFilter filter) {
        List<GdUpdateSmartFilterCondition> gdUpdateSmartFilterConditions = mapList(filter.getConditions(),
                SmartFilterGraphQlServiceUpdateTest::toGdUpdateSmartFilterCondition);
        return new GdUpdateSmartFiltersItem()
                .withId(filter.getId())
                .withName(filter.getName())
                .withTargetFunnel(toGdTargetFunnel(filter.getTargetFunnel()))
                .withPriceCpc(filter.getPriceCpc())
                .withPriceCpa(filter.getPriceCpa())
                .withAutobudgetPriority(toGdAutobudgetPriority(filter.getAutobudgetPriority()))
                .withIsSuspended(filter.getIsSuspended())
                .withConditions(gdUpdateSmartFilterConditions)
                .withTab(toGd(filter.getTab()));
    }

    private GdSmartFilterTab toGd(PerformanceFilterTab tab) {
        return Optional.ofNullable(tab)
                .map(t -> GdSmartFilterTab.valueOf(t.toString()))
                .orElse(null);
    }

    private static GdUpdateSmartFilterCondition toGdUpdateSmartFilterCondition(PerformanceFilterCondition condition) {
        return new GdUpdateSmartFilterCondition()
                .withField(condition.getFieldName())
                .withOperator(SmartFilterConverter.toGdConditionOperator(condition.getOperator()))
                .withStringValue(condition.getStringValue());
    }

}
