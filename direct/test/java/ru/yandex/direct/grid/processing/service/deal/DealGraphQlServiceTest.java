package ru.yandex.direct.grid.processing.service.deal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.deal.model.CompleteReason;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.service.DealService;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.DealSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.operation.Applicability;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestDeals.defaultPrivateDeal;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    deals(filter: {statusContains: [RECEIVED]}, selectedDealIds: %s, orderBy: [{field: NAME, order: " +
            "ASC}], limitOffset: {offset: %s, limit: %s}) {\n"
            + "      totalCount\n"
            + "      dealIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        index\n"
            + "        name\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private DealSteps dealSteps;

    @Autowired
    private DealService dealService;

    @Test
    public void testService() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        ClientId clientId = userInfo.getClientInfo().getClientId();
        List<DealInfo> dealInfos = dealSteps.addDeals(asList(
                (Deal) defaultPrivateDeal(clientId).withName("Name 0"),
                (Deal) defaultPrivateDeal(clientId).withName("Name 1"),
                (Deal) defaultPrivateDeal(clientId).withName("Name 2"),
                (Deal) defaultPrivateDeal(clientId).withName("Name 3")
        ), userInfo.getClientInfo());
        dealService.completeDeals(clientId, singletonList(dealInfos.get(0).getDealId()),
                CompleteReason.BY_CLIENT, Applicability.FULL);

        GridGraphQLContext operator = new GridGraphQLContext(userInfo.getUser());
        String query = String.format(QUERY_TEMPLATE, userInfo.getUser().getLogin(),
                asList(dealInfos.get(0).getDealId(), dealInfos.get(1).getDealId(), dealInfos.get(2).getDealId()), 1, 1);
        ExecutionResult result = processor.processQuery(null, query, null, operator);

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "deals", ImmutableMap.of(
                                "totalCount", 2,
                                "dealIds",
                                asList(dealInfos.get(1).getDealId(), dealInfos.get(2).getDealId()),
                                "rowset", singletonList(ImmutableMap.<String, Object>builder()
                                        .put("id", dealInfos.get(2).getDealId())
                                        .put("index", 1)
                                        .put("name", dealInfos.get(2).getDeal().getName())
                                        .build())
                        )
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }
}
