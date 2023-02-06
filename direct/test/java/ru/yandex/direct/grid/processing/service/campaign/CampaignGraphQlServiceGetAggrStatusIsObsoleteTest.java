package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
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

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на отдачу aggr_statuses_campaigns.is_obsolete параметра
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignGraphQlServiceGetAggrStatusIsObsoleteTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        aggregatedStatusInfo {\n"
            + "          isObsolete\n"
            + "        }"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String AGGR_DATA_ACTIVE = ""
            + "{\"r\": [\"CAMPAIGN_ACTIVE\"], "
            + "\"s\": \"RUN_OK\", "
            + "\"sts\": [\"PAYED\"], "
            + "\"cnts\": {\"s\": {\"RUN_OK\": 1}, "
            + "\"grps\": 1}}";

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
    private DslContextProvider dslContextProvider;

    private GridGraphQLContext context;
    private UserInfo userInfo;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long uid;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        clientId = userInfo.getClientId();
        uid = userInfo.getUid();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    public static Object[] parameters() {
        return new Object[][]{
                {true, ImmutableMap.of("isObsolete", true)},
                {false, ImmutableMap.of("isObsolete", false)},
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("is obsolete: {0}, expect: {1}")
    public void testGetIsObsolete(boolean isObsolete,
                                  Map<String, Object> mapIsObsoleteExpected) {
        var campaignId = createCampaignWithAggrStatus(isObsolete).getCampaignId();

        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expect = expectResult(campaignId, mapIsObsoleteExpected);
        assertThat(data).is(matchedBy(beanDiffer(expect)));
    }

    private Map<String, Object> expectResult(Long campaignId, Map<String, Object> mapToObsolete) {
        Map<String, Object> mapOfRowset = new HashMap<>();
        mapOfRowset.put("id", campaignId);
        mapOfRowset.put("aggregatedStatusInfo", mapToObsolete);
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "rowset", Collections.singletonList(mapOfRowset)
                        )
                ));
    }

    private CampaignInfo createCampaignWithAggrStatus(boolean isObsolete) {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(clientId, uid), clientInfo);
        createAggrStatusesCampaign(campaignInfo.getCampaignId(), isObsolete);
        return campaignInfo;
    }

    private void createAggrStatusesCampaign(Long cid, boolean isObsolete) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.CID, AGGR_STATUSES_CAMPAIGNS.AGGR_DATA,
                        AGGR_STATUSES_CAMPAIGNS.UPDATED, AGGR_STATUSES_CAMPAIGNS.IS_OBSOLETE)
                .values(cid, AGGR_DATA_ACTIVE, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), isObsolete ? 1L : 0L)
                .execute();
    }
}
