package ru.yandex.direct.grid.processing.service.smartfilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderBy;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderByField;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmartFilterGraphQlServiceOrderByTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {id: %s}) {\n"
            + "    smartFilters(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    private Steps steps;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private UserService userService;

    private PerformanceFilterInfo filterInfo1;
    private PerformanceFilterInfo filterInfo2;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClientAndUser();
        filterInfo1 = steps.performanceFilterSteps().createDefaultPerformanceFilter(clientInfo);
        filterInfo2 = steps.performanceFilterSteps().createDefaultPerformanceFilter(clientInfo);
    }

    @Test
    public void getSmartFilters_whenDescOrderByIsSuspended_success() {
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo1, PerformanceFilter.IS_SUSPENDED, true);

        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withOrder(Order.DESC)
                .withField(GdSmartFilterOrderByField.IS_SUSPENDED);
        Map<String, Object> data = sendRequest(singletonList(orderBy));

        Long firstId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/0/id");
        Long secondId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/1/id");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstId).as("the first id").isEqualTo(filterInfo1.getFilterId());
            soft.assertThat(secondId).as("the second id").isEqualTo(filterInfo2.getFilterId());
        });
    }

    @Test
    public void getSmartFilters_whenAscOrderByIsSuspended_success() {
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo1, PerformanceFilter.IS_SUSPENDED, true);

        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withOrder(Order.ASC)
                .withField(GdSmartFilterOrderByField.IS_SUSPENDED);
        Map<String, Object> data = sendRequest(singletonList(orderBy));

        Long firstId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/0/id");
        Long secondId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/1/id");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstId).as("the first id").isEqualTo(filterInfo2.getFilterId());
            soft.assertThat(secondId).as("the second id").isEqualTo(filterInfo1.getFilterId());
        });
    }

    @Test
    public void getSmartFilters_whenAscOrderByTargetFunnel_success() {
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo1, PerformanceFilter.TARGET_FUNNEL,
                TargetFunnel.SAME_PRODUCTS);
        checkState(filterInfo2.getFilter().getTargetFunnel()==TargetFunnel.NEW_AUDITORY);

        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withOrder(Order.ASC)
                .withField(GdSmartFilterOrderByField.TARGET_FUNNEL);
        Map<String, Object> data = sendRequest(singletonList(orderBy));

        Long firstId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/0/id");
        Long secondId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/1/id");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstId).as("the first id").isEqualTo(filterInfo1.getFilterId());
            soft.assertThat(secondId).as("the second id").isEqualTo(filterInfo2.getFilterId());
        });
    }

    @Test
    public void getSmartFilters_whenDescOrderByTargetFunnel_success() {
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo1, PerformanceFilter.TARGET_FUNNEL,
                TargetFunnel.SAME_PRODUCTS);
        checkState(filterInfo2.getFilter().getTargetFunnel()==TargetFunnel.NEW_AUDITORY);

        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withOrder(Order.DESC)
                .withField(GdSmartFilterOrderByField.TARGET_FUNNEL);
        Map<String, Object> data = sendRequest(singletonList(orderBy));

        Long firstId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/0/id");
        Long secondId = GraphQLUtils.getDataValue(data, "client/smartFilters/rowset/1/id");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstId).as("the first id").isEqualTo(filterInfo2.getFilterId());
            soft.assertThat(secondId).as("the second id").isEqualTo(filterInfo1.getFilterId());
        });
    }

    private Map<String, Object> sendRequest(List<GdSmartFilterOrderBy> orderBy) {
        User user = userService.getUser(clientInfo.getUid());
        GridGraphQLContext context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null);
        GdSmartFiltersContainer smartFiltersContainer = SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer();
        smartFiltersContainer.setOrderBy(orderBy);
        Set<Long> campaignIdIn = Set.of(filterInfo1.getCampaignId(), filterInfo2.getCampaignId());
        smartFiltersContainer.getFilter().withCampaignIdIn(campaignIdIn);
        String containerAsString = graphQlSerialize(smartFiltersContainer);
        String query = String.format(QUERY_TEMPLATE, clientInfo.getClientId(), containerAsString);
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

}
