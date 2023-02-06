package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
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
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

/**
 * Тесты на значения поля tags при запросе кампаний.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceTagsTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {userId: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        tags {\n"
            + "          id\n"
            + "          name\n"
            + "          usesCount\n"
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
    public void getTags_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        Tag tag = steps.tagCampaignSteps().createDefaultTag(campaignInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        Long id = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/id");
        String name = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/name");
        Integer usesCount = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/usesCount");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("id").isEqualTo(tag.getId());
            soft.assertThat(name).as("name").isEqualTo(tag.getName());
            soft.assertThat(usesCount).as("usesCount").isEqualTo(0);
        });
    }

    @Test
    public void getTags_usesCount_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        Tag tag = steps.tagCampaignSteps().createDefaultTag(campaignInfo);
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup();
        steps.tagCampaignSteps().addAdGroupTag(adGroupInfo1, tag);
        steps.tagCampaignSteps().addAdGroupTag(adGroupInfo2, tag);

        Map<String, Object> data = sendRequest(campaignInfo);

        Long id = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/id");
        String name = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/name");
        Integer usesCount = GraphQLUtils.getDataValue(data, "client/campaigns/rowset/0/tags/0/usesCount");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("id").isEqualTo(tag.getId());
            soft.assertThat(name).as("name").isEqualTo(tag.getName());
            soft.assertThat(usesCount).as("usesCount").isEqualTo(2);
        });
    }

    private Map<String, Object> sendRequest(CampaignInfo campaignInfo) {
        GridGraphQLContext context = getGridGraphQLContext(campaignInfo.getClientInfo().getUid());
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
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
