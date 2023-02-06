package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Select;
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
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на сервис, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceWithTagTest {

    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy:{id: %1$d}) {\n"
            + "    adGroups(input: %2$s) {\n"
            + "      totalCount\n"
            + "      adGroupIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        tags{\n"
            + "          id\n"
            + "          name\n"
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
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Test
    public void getGdSmartAdGroup_withNullTagFilter_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, null);

        //Сверяем ожидания и реальность
        Long id = GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/id");
        List<Object> tags = GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/tags");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("adGroupId").isEqualTo(adGroupInfo.getAdGroupId());
            soft.assertThat(tags).as("tags").isEmpty();
        });
    }

    @Test
    public void getGdSmartAdGroup_withEmptyTagFilter_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, emptySet());

        //Сверяем ожидания и реальность
        List<Object> rowset = GraphQLUtils.getDataValue(data, "client/adGroups/rowset");
        assertThat(rowset).isEmpty();
    }

    @Test
    public void getGdSmartAdGroup_whenGroupDosntHasTag_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Tag tag = steps.tagCampaignSteps().createDefaultTag(adGroupInfo.getCampaignInfo());

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, singleton(tag.getId()));

        //Сверяем ожидания и реальность
        List<Object> rowset = GraphQLUtils.getDataValue(data, "client/adGroups/rowset");
        assertThat(rowset).isEmpty();
    }

    @Test
    public void getGdSmartAdGroup_whenGroupHasTag_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Tag tag = steps.tagCampaignSteps().addDefaultTag(adGroupInfo);

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, singleton(tag.getId()));

        //Сверяем ожидания и реальность
        Long id = GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/id");
        Long tagId = GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/tags/0/id");
        String tagName = GraphQLUtils.getDataValue(data, "client/adGroups/rowset/0/tags/0/name");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("adGroupId").isEqualTo(adGroupInfo.getAdGroupId());
            soft.assertThat(tagId).as("tagId").isEqualTo(tag.getId());
            soft.assertThat(tagName).as("tagName").isEqualTo(tag.getName());
        });
    }

    private Map<String, Object> sendRequest(PerformanceAdGroupInfo adGroupInfo, Set<Long> tagIds) {
        String filter = getFilter(singletonList(adGroupInfo), tagIds);
        String query = String.format(QUERY_TEMPLATE, adGroupInfo.getClientInfo().getClientId().asLong(), filter);
        GridGraphQLContext context = getGridGraphQLContext(adGroupInfo.getClientInfo().getUid());
        gridContextProvider.setGridContext(context);
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

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }

    private String getFilter(List<PerformanceAdGroupInfo> adGroupInfos, Set<Long> tagIds) {
        Set<@GraphQLNonNull Long> adGroupIds = StreamEx.of(adGroupInfos)
                .map(PerformanceAdGroupInfo::getAdGroupId)
                .toSet();
        Set<@GraphQLNonNull Long> campaignIds = StreamEx.of(adGroupInfos)
                .map(PerformanceAdGroupInfo::getCampaignInfo)
                .map(CampaignInfo::getCampaignId)
                .toSet();
        GdAdGroupOrderBy orderById = new GdAdGroupOrderBy()
                .withField(GdAdGroupOrderByField.ID)
                .withOrder(Order.ASC);
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(adGroupIds)
                        .withCampaignIdIn(campaignIds)
                        .withTagIdIn(tagIds)
                )
                .withOrderBy(singletonList(orderById));
        return graphQlSerialize(adGroupsContainer);
    }

}
