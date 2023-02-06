package ru.yandex.direct.grid.processing.service.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import one.util.streamex.StreamEx;
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
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderBy;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.TestUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getDefaultCampaignsContainerInput;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;


/**
 * Тест lazy-полей к объектам типа GdCampaign.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceLazyFieldsTest {

    private static final String QUERY_TEMPLATE_DEFAULT_REGION_IDS = "{\n"
            + "  client(searchBy:{id: %1$d}) {\n"
            + "    campaigns(input: %2$s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        defaultRegionIds\n"
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
    private Steps steps;
    @Autowired
    private UserService userService;

    @Test
    public void getDefaultRegionIds_success() {
        //Создаём группу
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        TestUtils.assumeThat(campaignInfo.getCampaign().getGeo(), nullValue());
        TestUtils.assumeThat(campaignInfo.getClientInfo().getClient().getCountryRegionId(),
                is(Region.RUSSIA_REGION_ID));

        //Выполняем запрос
        Map<String, Object> data = sendRequest(campaignInfo, QUERY_TEMPLATE_DEFAULT_REGION_IDS);

        //Сверяем ожидания и реальность
        ArrayList<Long> defaultRegionIds = getDataValue(data, "client/campaigns/rowset/0/defaultRegionIds");
        assertThat(defaultRegionIds).as("defaultRegionIds").containsExactly(Region.RUSSIA_REGION_ID);
    }

    private Map<String, Object> sendRequest(CampaignInfo campaignInfo, String queryTemplate) {
        String filter = getFilter(singletonList(campaignInfo));
        String query = String.format(queryTemplate, campaignInfo.getClientInfo().getClientId().asLong(), filter);
        GridGraphQLContext context = getGridGraphQLContext(campaignInfo.getClientInfo().getUid());
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

    private String getFilter(List<CampaignInfo> campaignInfos) {
        Set<@GraphQLNonNull Long> campaignIds = StreamEx.of(campaignInfos)
                .map(CampaignInfo::getCampaignId)
                .toSet();
        GdCampaignOrderBy orderById = new GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.ID)
                .withOrder(Order.ASC);
        GdCampaignsContainer container = getDefaultCampaignsContainerInput()
                .withFilter(new GdCampaignFilter()
                        .withCampaignIdIn(campaignIds)
                )
                .withOrderBy(singletonList(orderById));
        return graphQlSerialize(container);
    }

}
