package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.PERFORMANCE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.TEXT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

/**
 * Тест на сервис, проверяем метод defaultCampaignMetrikaCounters, что счетчики клиента приходят, в зависимости от
 * переданного типа кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class ClientConstantsGraphQlServiceDefaultCampaignMetrikaCountersTest {

    private static final long COUNTER_ID_1 = 100;
    private static final long COUNTER_ID_2 = 200;
    private final static String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    clientConstants {\n" +
            "      defaultCampaignMetrikaCounters(campaignTypes: %s) {\n" +
            "        campaignType,\n" +
            "        counters\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private Steps steps;

    private User operator;
    private ClientInfo clientInfo;
    private GridGraphQLContext context;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        operator = userInfo.getUser();
        context = ContextHelper.buildContext(operator);
    }

    public static Object[] parameters() {
        return new Object[][]{
                {"У клиента один счетчик, запрашиваем по типу ТГО -> получаем один счетчик",
                        singletonList(COUNTER_ID_1), singletonList(TEXT),
                        Map.of(TEXT, singletonList((int) COUNTER_ID_1))},
                {"У клиента один счетчик, запрашиваем по типу СМАРТ -> получаем один счетчик",
                        singletonList(COUNTER_ID_1), singletonList(PERFORMANCE),
                        Map.of(PERFORMANCE, singletonList((int) COUNTER_ID_1))},
                {"У клиента два счетчика, запрашиваем по типу ТГО -> получаем два счетчика",
                        asList(COUNTER_ID_1, COUNTER_ID_2), singletonList(TEXT),
                        Map.of(TEXT, asList((int) COUNTER_ID_1, (int) COUNTER_ID_2))},
                {"У клиента два счетчика, запрашиваем по типу СМАРТ -> получаем один счетчик",
                        asList(COUNTER_ID_1, COUNTER_ID_2), singletonList(PERFORMANCE),
                        Map.of(PERFORMANCE, singletonList((int) COUNTER_ID_1))},
                {"У клиента нет счетчиков, запрашиваем по типу ТГО -> не получаем счетчиков",
                        emptyList(), singletonList(TEXT),
                        Map.of(TEXT, emptyList())},
                {"У клиента нет счетчиков, запрашиваем по типу СМАРТ -> не получаем счетчиков",
                        emptyList(), singletonList(PERFORMANCE),
                        Map.of(PERFORMANCE, emptyList())},
                {"У клиента один счетчик, запрашиваем по типу ТГО,СМАРТ -> получаем один счетчик для каждого типа",
                        singletonList(COUNTER_ID_1), asList(TEXT, PERFORMANCE),
                        Map.of(TEXT, singletonList((int) COUNTER_ID_1),
                                PERFORMANCE, singletonList((int) COUNTER_ID_1))},
                {"У клиента два счетчика, запрашиваем ТГО,СМАРТ -> получаем один счетчик для СМАРТ и два для ТГО",
                        asList(COUNTER_ID_1, COUNTER_ID_2), asList(TEXT, PERFORMANCE),
                        Map.of(TEXT, asList((int) COUNTER_ID_1, (int) COUNTER_ID_2),
                                PERFORMANCE, singletonList((int) COUNTER_ID_1))},
                {"У клиента нет счетчиков, запрашиваем ТГО,СМАРТ -> не получаем счетчиков",
                        emptyList(), asList(TEXT, PERFORMANCE),
                        Map.of(TEXT, emptyList(), PERFORMANCE, emptyList())},
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void defaultCampaignMetrikaCounters(@SuppressWarnings("unused") String description,
                                               List<Long> clientCounters,
                                               List<GdCampaignType> requestedCampaignTypes,
                                               Map<GdCampaignType, List<Integer>> expectCampaignTypeToCounters) {
        if (!clientCounters.isEmpty()) {
            steps.clientSteps().addCommonMetrikaCounters(clientInfo, clientCounters);
        }

        var query = String.format(QUERY_TEMPLATE, operator.getLogin(), requestedCampaignTypes);
        var result = processor.processQuery(null, query, null, context);
        var data = result.getData();
        assumeThat(data, notNullValue());

        List<Object> actualData = getDataValue(data, "client/clientConstants/defaultCampaignMetrikaCounters");
        Map<GdCampaignType, Object> mapData = StreamEx.of(actualData)
                .mapToEntry(d -> getDataValue(d, "counters"))
                .mapKeys(d -> (String) getDataValue(d, "campaignType"))
                .mapKeys(GdCampaignType::valueOf)
                .toMap();
        assertEquals(mapData, expectCampaignTypeToCounters);
    }

    /**
     * Если у клиента два счетчика и запрашиваем для Смарт типа -> получаем только оди первый счетчик
     */
    @Test
    public void defaultCampaignMetrikaCounters_ForSmartTypeGetFirstCounter() {
        steps.clientSteps().addCommonMetrikaCounters(clientInfo, List.of(COUNTER_ID_1, COUNTER_ID_2));

        var query = String.format(QUERY_TEMPLATE, operator.getLogin(), singletonList(PERFORMANCE));
        var result = processor.processQuery(null, query, null, context);
        var data = result.getData();
        assumeThat(data, notNullValue());

        var expectCampaignTypeToCounters = Map.of(PERFORMANCE, singletonList((int) COUNTER_ID_1));

        List<Object> actualData = getDataValue(data, "client/clientConstants/defaultCampaignMetrikaCounters");
        Map<GdCampaignType, Object> mapData = StreamEx.of(actualData)
                .mapToEntry(d -> getDataValue(d, "counters"))
                .mapKeys(d -> (String) getDataValue(d, "campaignType"))
                .mapKeys(GdCampaignType::valueOf)
                .toMap();
        assertEquals(mapData, expectCampaignTypeToCounters);
    }
}
