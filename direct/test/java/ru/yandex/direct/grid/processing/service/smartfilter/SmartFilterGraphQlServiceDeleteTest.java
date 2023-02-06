package ru.yandex.direct.grid.processing.service.smartfilter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdDeleteSmartFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdDeleteSmartFilterItem;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdDeleteSmartFilterPayload;
import ru.yandex.direct.grid.processing.model.smartfilter.mutation.GdDeleteSmartFilterPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmartFilterGraphQlServiceDeleteTest {

    private static final String DELETE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    deletedItems {\n"
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
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String DELETE_MUTATION_NAME = "deleteFilters";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;
    @Autowired
    Steps steps;
    @Autowired
    PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    UserRepository userRepository;

    private Integer shard;
    private Long filterId;
    private GridGraphQLContext context;
    private Long adGroupId;
    private Long feedId;
    private PerformanceAdGroupInfo adGroupInfo;
    private PerformanceFilterInfo filterInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        filterId = filterInfo.getFilterId();
        shard = filterInfo.getShard();
        adGroupId = filterInfo.getAdGroupId();
        feedId = filterInfo.getFeedId();
        adGroupInfo = filterInfo.getAdGroupInfo();
        clientInfo = filterInfo.getClientInfo();

        Long uid = filterInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(shard, singletonList(uid)).get(0);
        TestAuthHelper.setDirectAuthentication(user);
        context = buildContext(user);
    }

    @Test
    public void deleteFilter_success() {
        //Ожидаемый статус фильтра
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(filterId)
                .withIsDeleted(true);

        //Выполняем запрос
        GdDeleteSmartFilterPayload payload = executeDelete(filterId);

        //Проверяем результат
        Long returnedId = payload.getDeletedItems().get(0).getId();
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(filterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(filterId);
            soft.assertThat(actualFilter)
                    .is(matchedBy(beanDiffer(expectedFilter)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void deleteFilter_success_whenConditionsIsEmpty() {
        PerformanceFilter notValidFilter = defaultPerformanceFilter(adGroupId, feedId)
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withConditions(emptyList());
        filterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(notValidFilter);
        steps.performanceFilterSteps().addPerformanceFilter(filterInfo);
        Long notValidFilterId = notValidFilter.getId();

        //Ожидаемый статус фильтра
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(notValidFilterId)
                .withIsDeleted(true);

        //Выполняем запрос
        GdDeleteSmartFilterPayload payload = executeDelete(notValidFilterId);

        //Проверяем результат
        Long returnedId = payload.getDeletedItems().get(0).getId();
        PerformanceFilter actualFilter =
                performanceFilterRepository.getFiltersById(shard, singletonList(notValidFilterId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedId).isEqualTo(notValidFilterId);
            soft.assertThat(actualFilter)
                    .is(matchedBy(beanDiffer(expectedFilter)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void deleteFilter_failure_whenFilterNotExist() {
        //Ожидаемый результат
        GdDeleteSmartFilterPayload expectedPayload = new GdDeleteSmartFilterPayload()
                .withDeletedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(list(new GdDefect()
                                .withCode("DefectIds.OBJECT_NOT_FOUND")
                                .withPath("deleteItems[0].id")))
                        .withWarnings(list()));

        //Выполняем запрос
        long notValidFilterId = Long.MAX_VALUE - 1;
        GdDeleteSmartFilterPayload payload = executeDelete(notValidFilterId);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void deleteFilters_success_massForDifferentAdGroupId() {
        PerformanceAdGroupInfo secondAdGroupInfo =
                steps.adGroupSteps().addPerformanceAdGroup(new PerformanceAdGroupInfo().withClientInfo(clientInfo));
        PerformanceFilter secondFilter =
                defaultPerformanceFilter(secondAdGroupInfo.getAdGroupId(), secondAdGroupInfo.getFeedId());
        PerformanceFilterInfo secondFilterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(secondAdGroupInfo)
                .withFilter(secondFilter);
        steps.performanceFilterSteps().addPerformanceFilter(secondFilterInfo);
        List<PerformanceFilterInfo> filterInfos = list(filterInfo, secondFilterInfo);
        List<Long> filterIds = mapList(filterInfos, PerformanceFilterInfo::getFilterId);

        //Выполняем запрос
        GdDeleteSmartFilterPayload payload = executeDelete(filterIds);

        Long secondFilterId = secondFilterInfo.getFilterId();
        GdDeleteSmartFilterPayload expectedPayload = new GdDeleteSmartFilterPayload()
                .withDeletedItems(Arrays.asList(
                        new GdDeleteSmartFilterPayloadItem().withId(filterId),
                        new GdDeleteSmartFilterPayloadItem().withId(secondFilterId)))
                .withValidationResult(null);

        //Проверяем результат
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private GdDeleteSmartFilterPayload executeDelete(List<Long> filtersId) {
        List<GdDeleteSmartFilterItem> deletedItems = StreamEx.of(filtersId)
                .map(id -> new GdDeleteSmartFilterItem().withId(id))
                .toList();
        GdDeleteSmartFilter request = new GdDeleteSmartFilter().withDeleteItems(deletedItems);
        String query = String.format(DELETE_MUTATION_TEMPLATE, DELETE_MUTATION_NAME,
                GraphQlJsonUtils.graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        Map<String, Object> data = result.getData();
        checkState(data.containsKey(DELETE_MUTATION_NAME));
        return GraphQlJsonUtils.convertValue(data.get(DELETE_MUTATION_NAME), GdDeleteSmartFilterPayload.class);
    }

    private GdDeleteSmartFilterPayload executeDelete(Long filterId) {
        return executeDelete(singletonList(filterId));
    }
}
