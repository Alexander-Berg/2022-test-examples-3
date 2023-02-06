package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteGraphQLServiceCalltrackingMetrikaCountersTest {
    private static final String URL = "https://www.example.domain.com/something";
    private static final String DOMAIN = "example.domain.com";
    private static final int COUNTER_ID_1 = 131072;
    private static final int COUNTER_ID_2 = 88077;
    private static final int COUNTER_ID_3 = 7473;
    private static final int COUNTER_ID_4 = 65109;
    private static final int COUNTER_ID_6 = RandomNumberUtils.nextPositiveInteger();
    private static final String COUNTER_NAME_1 = "Медведи на велосипеде";
    private static final String COUNTER_NAME_2 = "Комарики на воздушном шарике";
    private static final String COUNTER_NAME_3 = "Раки на хромой собаке";
    private static final String COUNTER_NAME_6 = "Счетчик c доступом только на чтение";
    private static final String PHONE_1 = "+78005553535";
    private static ClientInfo clientInfo;

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    calltrackingMetrikaCounters(input: {\n" +
            "      url: \"%s\",\n" +
            "      counterIds: %s\n" +
            "    }) {\n" +
            "      counters {\n" +
            "        domain\n" +
            "        id\n" +
            "        name\n" +
            "      }\n" +
            "      isMetrikaAvailable\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private CalltrackingNumberClicksRepository calltrackingNumberClicksRepository;

    private GridGraphQLContext chiefContext;
    private GridGraphQLContext operatorContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        // set up chief
        long chiefUserUid = clientInfo.getChiefUserInfo().getUser().getUid();
        User chiefUser = userService.getUser(chiefUserUid);
        chiefContext = ContextHelper.buildContext(chiefUser);
        metrikaClientStub.addUserCounters(chiefUserUid,
                List.of(
                        buildCounter(COUNTER_ID_1, COUNTER_NAME_1, DOMAIN),
                        buildCounter(COUNTER_ID_2, COUNTER_NAME_2, DOMAIN),
                        buildCounter(COUNTER_ID_3, COUNTER_NAME_3, DOMAIN),
                        // ignored because not editable
                        buildCounter(COUNTER_ID_6, null, MetrikaCounterPermission.VIEW)
                )
        );

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByCounters(anyString(), anyCollection())).thenReturn(
                Map.of(
                        (long) COUNTER_ID_1,
                        Map.of(
                                "whatever phone", 3,
                                "another phone", 4
                        ),
                        (long) COUNTER_ID_2,
                        Map.of("some phone", 7)
                )
        );

        // set up operator
        long operatorUserUid = steps.userSteps().createRepresentative(clientInfo, RbacRepType.MAIN).getUser().getUid();
        User operatorUser = userService.getUser(operatorUserUid);
        operatorContext = ContextHelper.buildContext(operatorUser);
        metrikaClientStub.addUserCounters(operatorUserUid,
                List.of(
                        buildCounter(COUNTER_ID_1, COUNTER_NAME_1, DOMAIN),
                        // ignored because not editable
                        buildCounter(COUNTER_ID_6, null, MetrikaCounterPermission.VIEW)));

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByCounters(DOMAIN, List.of((long) COUNTER_ID_1))).thenReturn(
                Map.of(
                        (long) COUNTER_ID_1,
                        Map.of(
                                "whatever phone", 3,
                                "another phone", 4
                        )
                )
        );
    }

    @Test
    public void happyPath() {
        gridContextProvider.setGridContext(chiefContext);

        List<Integer> counterIds = List.of(COUNTER_ID_1, COUNTER_ID_2, COUNTER_ID_3, COUNTER_ID_4);
        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), URL,
                graphQlSerialize(counterIds));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingMetrikaCounters", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "counters", ImmutableList.of(
                                        // This one is first because it has 7 clicks and lower counter_id
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_2)
                                                .put("name", COUNTER_NAME_2)
                                                .build(),
                                        // This one is second because it has 7 clicks and higher counter_id
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_1)
                                                .put("name", COUNTER_NAME_1)
                                                .build(),
                                        // This one is last because there are no clicks
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_3)
                                                .put("name", COUNTER_NAME_3)
                                                .build()
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void notAllCounterForOperator() {
        gridContextProvider.setGridContext(operatorContext);

        List<Integer> counterIds = List.of(COUNTER_ID_1, COUNTER_ID_2, COUNTER_ID_3, COUNTER_ID_4);
        var query = String.format(QUERY_TEMPLATE, operatorContext.getOperator().getLogin(), URL,
                graphQlSerialize(counterIds));
        ExecutionResult result = processor.processQuery(null, query, null, operatorContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingMetrikaCounters", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "counters", ImmutableList.of(
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_1)
                                                .put("name", COUNTER_NAME_1)
                                                .build()
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void happyPath_addCalltrackingCounter() {
        DomainInfo domainInfo = steps.domainSteps().createDomain(new DomainInfo()
                .withShard(clientInfo.getShard())
                .withDomain(new Domain().withDomain(DOMAIN)));
        steps.calltrackingSettingsSteps().add(clientInfo.getClientId(),
                domainInfo.getDomainId(),
                (long) COUNTER_ID_3,
                List.of(PHONE_1));
        gridContextProvider.setGridContext(chiefContext);

        List<Integer> counterIds = List.of(COUNTER_ID_1, COUNTER_ID_2, COUNTER_ID_4);
        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), URL,
                graphQlSerialize(counterIds));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingMetrikaCounters", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "counters", ImmutableList.of(
                                        // This one is first because it is calltracking_settings counter
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_3)
                                                .put("name", COUNTER_NAME_3)
                                                .build(),
                                        // This one is second because it has 7 clicks and lower counter_id
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_2)
                                                .put("name", COUNTER_NAME_2)
                                                .build(),
                                        // This one is third because it has 7 clicks and higher counter_id
                                        ImmutableMap.builder()
                                                .put("domain", DOMAIN)
                                                .put("id", (long) COUNTER_ID_1)
                                                .put("name", COUNTER_NAME_1)
                                                .build()
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }
}
