package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.feature.FeatureName.ADGROUP_INDIVISIBLE_DRAFT_STATUS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тесты на значения поля access/bannerActions при запросе групп.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceBannerActionsTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {userId: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        access {\n"
            + "          bannerActions\n"
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
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private UserService userService;
    @Autowired
    private Steps steps;

    public static UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_MODERATE.getName(),
                                info.getAdGroup().getStatusModerate().name())
        ));

        return builder.build();
    }

    @Test
    public void getBannerActions_whenAdGroupIsActive_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ADGROUP_INDIVISIBLE_DRAFT_STATUS, false);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        Map<String, Object> data = sendRequest(adGroupInfo);

        List<String> bannerActions =
                GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/access/bannerActions");
        assertThat(bannerActions).containsExactlyInAnyOrder("SAVE_AND_MODERATE");
    }

    @Test
    public void getBannerActions_whenAdGroupIsDraft_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ADGROUP_INDIVISIBLE_DRAFT_STATUS, false);
        CampaignInfo campaignInfo = steps.campaignSteps().createDraftPerformanceCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDraftPerformanceAdGroup(campaignInfo);

        Map<String, Object> data = sendRequest(adGroupInfo);

        List<String> bannerActions =
                GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/access/bannerActions");
        assertThat(bannerActions).containsExactlyInAnyOrder("SAVE_AND_MODERATE");
    }

    @Test
    public void getBannerActions_whenAdGroupIsActiveAndFeatureIsOn_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ADGROUP_INDIVISIBLE_DRAFT_STATUS, true);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        Map<String, Object> data = sendRequest(adGroupInfo);

        List<String> bannerActions =
                GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/access/bannerActions");
        assertThat(bannerActions).containsExactlyInAnyOrder("SAVE_AND_MODERATE");
    }

    @Test
    public void getBannerActions_whenAdGroupIsDraftAndFeatureIsOn_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ADGROUP_INDIVISIBLE_DRAFT_STATUS, true);
        CampaignInfo campaignInfo = steps.campaignSteps().createDraftPerformanceCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDraftPerformanceAdGroup(campaignInfo);

        Map<String, Object> data = sendRequest(adGroupInfo);

        List<String> bannerActions =
                GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/access/bannerActions");
        assertThat(bannerActions).containsExactlyInAnyOrder("SAVE_AS_DRAFT", "SAVE_AND_MODERATE_GROUP");
    }

    private Map<String, Object> sendRequest(AdGroupInfo adGroupInfo) {
        mockGridYt(adGroupInfo);
        GridGraphQLContext context = getGridGraphQLContext(adGroupInfo.getClientInfo().getUid());
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer();
        adGroupsContainer.getFilter().setCampaignIdIn(singleton(adGroupInfo.getCampaignId()));
        adGroupsContainer.getFilter().setAdGroupIdIn(singleton(adGroupInfo.getAdGroupId()));
        String serializedContainer = graphQlSerialize(adGroupsContainer);
        String query = String.format(QUERY_TEMPLATE, adGroupInfo.getClientInfo().getUid(), serializedContainer);
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private void mockGridYt(AdGroupInfo adGroupInfo) {
        UnversionedRowset rowset = convertToGroupsRowset(singletonList(adGroupInfo));
        doReturn(rowset).when(gridYtSupport).selectRows(eq(adGroupInfo.getShard()), any(Select.class), anyBoolean());
    }

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }

}
