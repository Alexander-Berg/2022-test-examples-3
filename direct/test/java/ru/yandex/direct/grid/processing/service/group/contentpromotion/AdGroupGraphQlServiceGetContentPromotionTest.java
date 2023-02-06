package ru.yandex.direct.grid.processing.service.group.contentpromotion;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.mutation.GdContentPromotionGroupType;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.EDA;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_COLLECTION;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_EDA;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_SERVICE;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceGetContentPromotionTest {

    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy:{id: %1$d}) {\n"
            + "    adGroups(input: %2$s) {\n"
            + "      totalCount\n"
            + "      adGroupIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        type\n"
            + "        ... on GdContentPromotionAdGroup {"
            + "          contentPromotionGroupType"
            + "        }\n"
            + "        campaign {\n"
            + "          id\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private GridContextProvider gridContextProvider;

    private ClientInfo clientInfo;
    private AdGroupInfo contentPromotionVideoAdGroupInfo;
    private AdGroupInfo contentPromotionCollectionAdGroupInfo;
    private AdGroupInfo contentPromotionServiceAdGroupInfo;
    private AdGroupInfo contentPromotionEdaAdGroupInfo;

    private GridGraphQLContext context;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);

        contentPromotionVideoAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, VIDEO);
        contentPromotionCollectionAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, COLLECTION);
        contentPromotionServiceAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, SERVICE);
        contentPromotionEdaAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, EDA);

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void adGroups_ContentPromotionVideo_WithVideoFeatureOn_AdGroupReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 1,
                                "adGroupIds", list(contentPromotionVideoAdGroupInfo.getAdGroupId()),
                                "rowset", list(
                                        map(
                                                "id", contentPromotionVideoAdGroupInfo.getAdGroupId(),
                                                "type", CONTENT_PROMOTION_VIDEO.name(),
                                                "contentPromotionGroupType", GdContentPromotionGroupType.VIDEO.name(),
                                                "campaign", map(
                                                        "id", contentPromotionVideoAdGroupInfo.getCampaignId()
                                                )
                                        )
                                )
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionVideoAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionCollection_WithCollectionsFeatureOn_AdGroupReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 1,
                                "adGroupIds", list(contentPromotionCollectionAdGroupInfo.getAdGroupId()),
                                "rowset", list(
                                        map(
                                                "id", contentPromotionCollectionAdGroupInfo.getAdGroupId(),
                                                "type", CONTENT_PROMOTION_COLLECTION.name(),
                                                "contentPromotionGroupType", GdContentPromotionGroupType.COLLECTION.name(),
                                                "campaign", map(
                                                        "id", contentPromotionCollectionAdGroupInfo.getCampaignId()
                                                )
                                        )
                                )
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionCollectionAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionVideo_WithVideoFeatureOff_AdGroupNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 0,
                                "adGroupIds", list(),
                                "rowset", list()
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionVideoAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionCollection_WithCollectionsFeatureOff_AdGroupNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 0,
                                "adGroupIds", list(),
                                "rowset", list()
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionCollectionAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionService_WithServiceFeatureOn_AdGroupReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, true);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 1,
                                "adGroupIds", list(contentPromotionServiceAdGroupInfo.getAdGroupId()),
                                "rowset", list(
                                        map(
                                                "id", contentPromotionServiceAdGroupInfo.getAdGroupId(),
                                                "type", CONTENT_PROMOTION_SERVICE.name(),
                                                "contentPromotionGroupType", GdContentPromotionGroupType.SERVICE.name(),
                                                "campaign", map(
                                                        "id", contentPromotionServiceAdGroupInfo.getCampaignId()
                                                )
                                        )
                                )
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionServiceAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionService_WithServiceFeatureOff_AdGroupNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, false);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 0,
                                "adGroupIds", list(),
                                "rowset", list()
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionServiceAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionEda_WithEdaFeatureOn_AdGroupReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_EDA_INTERFACE, true);


        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 1,
                                "adGroupIds", list(contentPromotionEdaAdGroupInfo.getAdGroupId()),
                                "rowset", list(
                                        map(
                                                "id", contentPromotionEdaAdGroupInfo.getAdGroupId(),
                                                "type", CONTENT_PROMOTION_EDA.name(),
                                                "contentPromotionGroupType", GdContentPromotionGroupType.EDA.name(),
                                                "campaign", map(
                                                        "id", contentPromotionEdaAdGroupInfo.getCampaignId()
                                                )
                                        )
                                )
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionEdaAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void adGroups_ContentPromotionEda_WithEdaFeatureOff_AdGroupNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CONTENT_PROMOTION_EDA_INTERFACE, false);

        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 0,
                                "adGroupIds", list(),
                                "rowset", list()
                        )
                ));

        Map<String, Object> data = sendRequest(contentPromotionEdaAdGroupInfo, QUERY_TEMPLATE);

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private Map<String, Object> sendRequest(AdGroupInfo adGroupInfo, String queryTemplate) {
        String filter = getFilter(singletonList(adGroupInfo));
        String query = String.format(queryTemplate, adGroupInfo.getClientInfo().getClientId().asLong(), filter);
        UnversionedRowset ytRowset = convertToGroupsRowset(singletonList(adGroupInfo));
        doReturn(ytRowset)
                .when(gridYtSupport).selectRows(eq(adGroupInfo.getShard()), any(Select.class), anyBoolean());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
        ));

        return builder.build();
    }

    private String getFilter(List<AdGroupInfo> adGroupInfos) {
        Set<@GraphQLNonNull Long> adGroupIds = StreamEx.of(adGroupInfos)
                .map(AdGroupInfo::getAdGroupId)
                .toSet();
        Set<@GraphQLNonNull Long> campaignIds = StreamEx.of(adGroupInfos)
                .map(AdGroupInfo::getCampaignInfo)
                .map(CampaignInfo::getCampaignId)
                .toSet();
        GdAdGroupOrderBy orderById = new GdAdGroupOrderBy()
                .withField(GdAdGroupOrderByField.ID)
                .withOrder(Order.ASC);
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(adGroupIds)
                        .withCampaignIdIn(campaignIds)
                )
                .withOrderBy(singletonList(orderById));
        return graphQlSerialize(adGroupsContainer);
    }
}
