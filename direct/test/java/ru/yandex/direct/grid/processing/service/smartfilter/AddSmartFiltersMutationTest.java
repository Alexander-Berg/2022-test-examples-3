package ru.yandex.direct.grid.processing.service.smartfilter;

import java.math.BigDecimal;
import java.util.Map;

import graphql.ExecutionResult;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.NowOptimizingBy;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterConditionOperator;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTab;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTargetFunnel;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFilterCondition;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFilters;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFiltersItem;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFiltersPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddSmartFiltersMutationTest {

    private static final String MUTATION_NAME = "addSmartFilters";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedItems {"
            + "      id"
            + "    }\n"
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

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private PerformanceFilterStorage performanceFilterStorage;

    private User operator;
    private int shard;
    private Long adGroupId;
    private PerformanceAdGroupInfo adGroupInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        adGroupId = adGroupInfo.getAdGroupId();

        shard = adGroupInfo.getShard();
        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addSmartFilters() {
        GdAddSmartFiltersItem filterToAdd = new GdAddSmartFiltersItem()
                .withAdGroupId(adGroupId)
                .withName("test name")
                .withIsSuspended(false)
                .withTargetFunnel(GdSmartFilterTargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(new BigDecimal("10.5"))
                .withPriceCpa(null)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withTab(GdSmartFilterTab.CONDITION)
                .withConditions(asList(
                        new GdAddSmartFilterCondition()
                                .withField("name")
                                .withOperator(GdSmartFilterConditionOperator.CONTAINS_ANY)
                                .withStringValue("[\"Платье\"]"),
                        new GdAddSmartFilterCondition()
                                .withField("categoryId")
                                .withOperator(GdSmartFilterConditionOperator.EQUALS_ANY)
                                .withStringValue("[111, 333]"),
                        new GdAddSmartFilterCondition()
                                .withField("typePrefix")
                                .withOperator(GdSmartFilterConditionOperator.EXISTS)
                                .withStringValue("1")
                ));
        GdAddSmartFiltersPayload gdAddSmartFiltersPayload = addFilter(filterToAdd);
        validateAddSuccessful(gdAddSmartFiltersPayload);

        Long filterId = gdAddSmartFiltersPayload.getAddedItems().get(0).getId();

        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withPid(adGroupId)
                .withName("test name")
                .withIsSuspended(false)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(new BigDecimal("10.5"))
                .withPriceCpa(BigDecimal.ZERO)
                .withAutobudgetPriority(1)
                .withTab(PerformanceFilterTab.CONDITION)
                .withNowOptimizingBy(NowOptimizingBy.CPC)
                .withIsDeleted(false)
                .withFeedType(FeedType.YANDEX_MARKET)
                .withBusinessType(BusinessType.RETAIL);

        String conditions = "{\n"
                + "  \"name ilike\": [\n"
                + "    \"Платье\"\n"
                + "  ],\n"
                + "  \"categoryId ==\": [\n"
                + "    111,\n"
                + "    333\n"
                + "  ],\n"
                + "  \"typePrefix exists\": \"1\"\n"
                + "}";
        expectedFilter.withConditions(
                PerformanceFilterConditionDBFormatParser.INSTANCE
                        .parse(performanceFilterStorage.getFilterSchema(expectedFilter), conditions)
        );

        checkFilter(filterId, expectedFilter);
    }

    @Test
    public void addSmartFilters_failure_whenNewNameIsTooLong() {
        GdAddSmartFiltersItem filterToAdd = new GdAddSmartFiltersItem()
                .withAdGroupId(adGroupId)
                .withName(StringUtils.repeat('A', 101))
                .withIsSuspended(false)
                .withTargetFunnel(GdSmartFilterTargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(new BigDecimal("10.5"))
                .withPriceCpa(null)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(asList(
                        new GdAddSmartFilterCondition()
                                .withField("name")
                                .withOperator(GdSmartFilterConditionOperator.CONTAINS_ANY)
                                .withStringValue("[\"Платье\"]"),
                        new GdAddSmartFilterCondition()
                                .withField("categoryId")
                                .withOperator(GdSmartFilterConditionOperator.EQUALS_ANY)
                                .withStringValue("[111, 333]"),
                        new GdAddSmartFilterCondition()
                                .withField("typePrefix")
                                .withOperator(GdSmartFilterConditionOperator.EXISTS)
                                .withStringValue("1")
                ));

        //Ожидаемый результат
        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX")
                                .withParams(map("maxLength", 100))
                                .withPath("addItems[0].name")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdAddSmartFiltersPayload payload = addFilter(filterToAdd);

        //Проверяем результат
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addSmartFilters_success_whenPriceCpcAndPriceCpaNotSet() {
        //Исходные данные
        GdAddSmartFiltersItem filterToAdd = new GdAddSmartFiltersItem()
                .withAdGroupId(adGroupId)
                .withName("Test name")
                .withIsSuspended(false)
                .withTargetFunnel(GdSmartFilterTargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(null)
                .withPriceCpa(null)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(asList(
                        new GdAddSmartFilterCondition()
                                .withField("name")
                                .withOperator(GdSmartFilterConditionOperator.CONTAINS_ANY)
                                .withStringValue("[\"Платье\"]"),
                        new GdAddSmartFilterCondition()
                                .withField("categoryId")
                                .withOperator(GdSmartFilterConditionOperator.EQUALS_ANY)
                                .withStringValue("[111, 333]"),
                        new GdAddSmartFilterCondition()
                                .withField("typePrefix")
                                .withOperator(GdSmartFilterConditionOperator.EXISTS)
                                .withStringValue("1")
                ));

        //Ожидаемый результат
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withPriceCpc(BigDecimal.ZERO)
                .withPriceCpa(BigDecimal.ZERO);

        //Выполняем запрос
        GdAddSmartFiltersPayload payload = addFilter(filterToAdd);

        //Проверяем результат
        Long filterId = payload.getAddedItems().get(0).getId();
        checkFilter(filterId, expectedFilter);
    }

    @Test
    public void addSmartFilters_failure_whenParsedValueIsWrong() {
        //Исходные данные
        GdAddSmartFiltersItem filterToAdd = new GdAddSmartFiltersItem()
                .withAdGroupId(adGroupId)
                .withName("Test name")
                .withIsSuspended(false)
                .withTargetFunnel(GdSmartFilterTargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(null)
                .withPriceCpa(null)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(singletonList(
                        new GdAddSmartFilterCondition()
                                .withField("price")
                                .withOperator(GdSmartFilterConditionOperator.RANGE)
                                .withStringValue("[\"20-10\"]")
                ));

        //Ожидаемый результат
        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("DefectIds.INVALID_VALUE")
                                .withPath("addItems[0].conditions[0].stringValue")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdAddSmartFiltersPayload payload = addFilter(filterToAdd);

        //Проверяем результат
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addSmartFilters_failure_whenFilterCountIsTooLarge() {
        //Создаём исходные данные и состояние группы
        int maxFilterCount = 50;
        for (int i = 0; i < maxFilterCount; i++) {
            steps.performanceFilterSteps()
                    .addPerformanceFilter(new PerformanceFilterInfo().withAdGroupInfo(adGroupInfo));
        }
        GdAddSmartFiltersItem filterToAdd = new GdAddSmartFiltersItem()
                .withAdGroupId(adGroupId)
                .withName("Test name")
                .withIsSuspended(false)
                .withTargetFunnel(GdSmartFilterTargetFunnel.PRODUCT_PAGE_VISIT)
                .withPriceCpc(null)
                .withPriceCpa(null)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(singletonList(
                        new GdAddSmartFilterCondition()
                                .withField("price")
                                .withOperator(GdSmartFilterConditionOperator.RANGE)
                                .withStringValue("[\"10-20\"]")
                ));

        //Ожидаемый результат
        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode(
                                        "PerformanceFilterDefects.PerformanceFilterNumberDefectIds.FILTER_COUNT_IS_TOO_LARGE")
                                .withParams(map("max", maxFilterCount))
                                .withPath("addItems[0]")))
                        .withWarnings(list()));

        //Выполняем запрос
        GdAddSmartFiltersPayload payload = addFilter(filterToAdd);

        //Проверяем результат
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }


    private GdAddSmartFiltersPayload addFilter(GdAddSmartFiltersItem filterToAdd) {
        GdAddSmartFilters gdAddSmartFilters = new GdAddSmartFilters()
                .withAddItems(singletonList(filterToAdd));

        return processQueryAndGetResult(createQuery(gdAddSmartFilters));
    }

    private String createQuery(GdAddSmartFilters gdAddSmartFilters) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddSmartFilters));
    }

    private GdAddSmartFiltersPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddSmartFiltersPayload.class);
    }

    private void validateAddSuccessful(GdAddSmartFiltersPayload actualGdAddSmartFiltersPayload) {
        assertThat(actualGdAddSmartFiltersPayload.getValidationResult()).isNull();
    }

    private void checkFilter(Long filterId, PerformanceFilter expectedFilter) {
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);

        CompareStrategy compareStrategy = onlyExpectedFields()
                .forFields(newPath("priceCpa"), newPath("priceCpc")).useDiffer(new BigDecimalDiffer());

        compareFilters(actualFilter, expectedFilter, compareStrategy);
    }

}
