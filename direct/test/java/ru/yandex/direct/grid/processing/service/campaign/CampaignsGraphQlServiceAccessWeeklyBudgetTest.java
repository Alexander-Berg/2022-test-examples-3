package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

/**
 * Тесты на значения поля access для действий по обновлению недельного бюджета при запросе кампаний.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceAccessWeeklyBudgetTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {userId: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        access {\n"
            + "          canEdit\n"
            + "          noActions\n"
            + "          actions\n"
            + "          pseudoActions\n"
            + "          servicedState\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private UserService userService;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void before(){
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void testWeeklyBudgetActions_whenAutobudgetCampaignIsActive_success() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignWithAvgCpaStrategy(clientInfo);

        Map<String, Object> data = sendRequest(clientInfo, campaignInfo.getCampaignId());

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).contains(GdiCampaignAction.EDIT_WEEKLY_BUDGET.name());
        assertThat(actions).contains(GdiCampaignAction.DISABLE_WEEKLY_BUDGET.name());
    }

    @Test
    public void testWeeklyBudgetActions_whenAutobudgetCampaignIsArchived_success() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignWithAvgCpaStrategy(clientInfo);
        steps.campaignSteps().archiveCampaign(campaignInfo);

        Map<String, Object> data = sendRequest(clientInfo, campaignInfo.getCampaignId());

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).doesNotContain(GdiCampaignAction.EDIT_WEEKLY_BUDGET.name());
        assertThat(actions).contains(GdiCampaignAction.DISABLE_WEEKLY_BUDGET.name()); //тип стратегии позволяет отключать НБ
    }

    @Test
    public void testWeeklyBudgetActions_whenNonAutobudgetCampaign_success() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        Map<String, Object> data = sendRequest(clientInfo, campaignInfo.getCampaignId());

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).doesNotContain(GdiCampaignAction.EDIT_WEEKLY_BUDGET.name());
        assertThat(actions).doesNotContain(GdiCampaignAction.DISABLE_WEEKLY_BUDGET.name());
    }

    private Map<String, Object> sendRequest(ClientInfo clientInfo, Long cid) {
        GridGraphQLContext context = getGridGraphQLContext(clientInfo.getUid());
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().withArchived(null);
        campaignsContainer.getFilter().setCampaignIdIn(singleton(cid));
        String containerAsString = graphQlSerialize(campaignsContainer);
        String query = String.format(QUERY_TEMPLATE, clientInfo.getUid(), containerAsString);
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }

}
