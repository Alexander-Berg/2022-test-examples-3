package ru.yandex.direct.grid.processing.service.smartfilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTab;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFilterCondition;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFilters;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFiltersItem;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFiltersPayload;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdAddSmartFiltersPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.smartfilter.SmartFilterConverter.toGdAutobudgetPriority;
import static ru.yandex.direct.grid.processing.service.smartfilter.SmartFilterConverter.toGdTargetFunnel;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmartFilterGraphQlServiceAddTest {

    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    addedItems {\n"
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
    private static final String ADD_MUTATION_NAME = "addSmartFilters";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adGroupId;
    private Integer shard;
    private Long feedId;
    private GridGraphQLContext context;

    @Before
    public void before() {
        PerformanceFilterInfo filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        feedId = filterInfo.getFeedId();
        adGroupId = filterInfo.getAdGroupId();
        shard = filterInfo.getShard();

        Long uid = filterInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(shard, singletonList(uid)).get(0);
        TestAuthHelper.setDirectAuthentication(user);
        context = buildContext(user);
    }

    @Test
    public void addFilter_failure_whenFiltersNotUnique() {
        // Подготавливаем исходные данные
        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, feedId);

        //Ожидаемый результат
        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_OBJECTS")
                                .withPath("addItems[0]")))
                        .withWarnings(list()));

        GdAddSmartFiltersPayload payload = executeAdd(secondFilter);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addFilter_success_whenFiltersUnique() {
        // Подготавливаем исходные данные
        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT);
        //Выполняем запрос
        GdAddSmartFiltersPayload payload = executeAdd(secondFilter);

        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withAddedItems(singletonList(
                        new GdAddSmartFiltersPayloadItem().withId(secondFilter.getId())))
                .withValidationResult(null);

        //Проверяем результат
        List<PerformanceFilter> filters =
                performanceFilterRepository.getNotDeletedFiltersByAdGroupIds(shard, singletonList(adGroupId))
                        .get(adGroupId);
        assertThat(filters.size()).isEqualTo(2);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addFilter_success_whenFiltersUniqueAndDuplicatedFilterIsDeleted() {
        // Подготавливаем исходные данные
        PerformanceFilter deletedFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT)
                .withIsDeleted(true);
        performanceFilterRepository.addPerformanceFilters(shard, singletonList(deletedFilter));

        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTargetFunnel(TargetFunnel.PRODUCT_PAGE_VISIT);
        //Выполняем запрос
        GdAddSmartFiltersPayload payload = executeAdd(secondFilter);

        GdAddSmartFiltersPayload expectedPayload = new GdAddSmartFiltersPayload()
                .withAddedItems(singletonList(
                        new GdAddSmartFiltersPayloadItem().withId(secondFilter.getId())))
                .withValidationResult(null);

        //Проверяем результат
        List<PerformanceFilter> filters =
                performanceFilterRepository.getNotDeletedFiltersByAdGroupIds(shard, singletonList(adGroupId))
                        .get(adGroupId);
        assertThat(filters.size()).isEqualTo(2);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private GdAddSmartFiltersPayload executeAdd(PerformanceFilter addFilter) {
        GdAddSmartFiltersItem item = toGdAddSmartFiltersItem(addFilter);
        List<@GraphQLNonNull GdAddSmartFiltersItem> addItems = singletonList(item);
        return sendRequest(addItems);
    }


    private GdAddSmartFiltersPayload sendRequest(List<@GraphQLNonNull GdAddSmartFiltersItem> addItems) {
        GdAddSmartFilters request = new GdAddSmartFilters().withAddItems(addItems);
        String query = String.format(ADD_MUTATION_TEMPLATE, ADD_MUTATION_NAME,
                GraphQlJsonUtils.graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        checkState(data.containsKey(ADD_MUTATION_NAME));
        return GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddSmartFiltersPayload.class);
    }

    private GdAddSmartFiltersItem toGdAddSmartFiltersItem(PerformanceFilter filter) {
        List<GdAddSmartFilterCondition> gdAddSmartFilterConditions = mapList(filter.getConditions(),
                SmartFilterGraphQlServiceAddTest::toGdAddSmartFilterCondition);
        return new GdAddSmartFiltersItem()
                .withAdGroupId(filter.getPid())
                .withName(filter.getName())
                .withTargetFunnel(toGdTargetFunnel(filter.getTargetFunnel()))
                .withPriceCpc(filter.getPriceCpc())
                .withPriceCpa(filter.getPriceCpa())
                .withAutobudgetPriority(toGdAutobudgetPriority(filter.getAutobudgetPriority()))
                .withIsSuspended(filter.getIsSuspended())
                .withConditions(gdAddSmartFilterConditions)
                .withTab(toGd(filter.getTab()));
    }

    private GdSmartFilterTab toGd(PerformanceFilterTab tab) {
        return Optional.ofNullable(tab)
                .map(t -> GdSmartFilterTab.valueOf(t.toString()))
                .orElse(null);
    }

    private static GdAddSmartFilterCondition toGdAddSmartFilterCondition(PerformanceFilterCondition condition) {
        return new GdAddSmartFilterCondition()
                .withField(condition.getFieldName())
                .withOperator(SmartFilterConverter.toGdConditionOperator(condition.getOperator()))
                .withStringValue(condition.getStringValue());
    }
}
