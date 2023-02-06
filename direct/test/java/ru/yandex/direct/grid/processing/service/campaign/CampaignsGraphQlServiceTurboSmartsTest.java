package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

/**
 * Тесты на значения поля turbo_smarts при запросе кампаний.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignsGraphQlServiceTurboSmartsTest {

    private static final Long COUNTER_ID = 5L;
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdSmartCampaign {\n"
            + "             hasTurboSmarts\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private Steps steps;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    private ClientInfo clientInfo;
    private Long uid;
    private int shard;
    private ClientId clientId;
    private GridGraphQLContext context;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();
        metrikaClientStub.addUserCounter(uid, COUNTER_ID.intValue());

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    public static Object[] parameters() {
        return new Object[][]{
                {null, false},
                {false, false},
                {true, true}
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("has turbo smarts: {0}, expect: {1}")
    public void test_WithTurboSmarts(Boolean hasTurboSmarts, Boolean expectHasTurboSmarts) {
        Long campaignId = createSmartCampaign(hasTurboSmarts);

        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        checkState(result.getErrors().isEmpty(), "Unexpected error in response");

        Map<String, Object> data = result.getData();
        Long dataId = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/id");
        Boolean dataHasTurboSmarts = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/hasTurboSmarts");

        SoftAssertions.assertSoftly(soft -> {
            assertThat(dataId).as("returned campaign id")
                    .isEqualTo(campaignId);
            assertThat(dataHasTurboSmarts).as("returned hasTurboSmarts")
                    .isEqualTo(expectHasTurboSmarts);
        });
    }

    private Long createSmartCampaign(Boolean hasTurboSmarts) {
        SmartCampaign campaign = TestCampaigns.defaultSmartCampaignWithSystemFields(clientInfo)
                .withMetrikaCounters(List.of(COUNTER_ID))
                .withHasTurboSmarts(hasTurboSmarts);

        var addParametersContainer = RestrictedCampaignsAddOperationContainer.create(shard, uid, clientId, uid, uid);
        List<Long> campaignIds = campaignModifyRepository.addCampaigns(dslContextProvider.ppc(shard),
                addParametersContainer, singletonList(campaign));
        return campaignIds.get(0);
    }
}
