package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
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
 * Тесты на значения поля access при запросе кампаний.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceAccessTest {

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

    @Test
    public void testExportInExcelAction_whenCampaignIsActive_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).contains("EXPORT_IN_EXCEL");
    }

    @Test
    public void testExportInExcelAction_whenCampaignArchived_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campaignSteps().archiveCampaign(campaignInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).doesNotContain("EXPORT_IN_EXCEL");
    }

    @Test
    public void testExportInExcelAction_whenMobileCampaign_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).contains("EXPORT_IN_EXCEL");
    }

    @Test
    public void testExportInExcelAction_whenPerformanceCampaign_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        List<String> actions = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/access/actions");
        assertThat(actions).doesNotContain("EXPORT_IN_EXCEL");
    }

    private Map<String, Object> sendRequest(CampaignInfo campaignInfo) {
        GridGraphQLContext context = getGridGraphQLContext(campaignInfo.getClientInfo().getUid());
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().withArchived(null);
        campaignsContainer.getFilter().setCampaignIdIn(singleton(campaignInfo.getCampaignId()));
        String containerAsString = graphQlSerialize(campaignsContainer);
        String query = String.format(QUERY_TEMPLATE, campaignInfo.getClientInfo().getUid(), containerAsString);
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
