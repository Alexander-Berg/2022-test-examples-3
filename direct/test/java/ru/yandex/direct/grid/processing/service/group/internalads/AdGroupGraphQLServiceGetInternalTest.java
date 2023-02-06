package ru.yandex.direct.grid.processing.service.group.internalads;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
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
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validIsMobileTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validTimeTargeting;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class AdGroupGraphQLServiceGetInternalTest {

    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      cacheKey\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdInternalAdGroup {\n"
            + "             targetings {\n"
            + "               __typename\n"
            + "             }\n"
            + "         }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GridGraphQLContext context;
    private AdGroupInfo groupInfo;
    private GdAdGroupsContainer adGroupsContainer;
    private UserInfo productUserInfo;
    private UserInfo operatorInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Before
    public void before() {
        steps.placementSteps().clearPlacements();
        productUserInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct().getChiefUserInfo();

        groupInfo = steps.adGroupSteps().createActiveInternalAdGroup(productUserInfo.getClientInfo());
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(
                groupInfo, List.of(validIsMobileTargeting(), validTimeTargeting()));


        doAnswer((Answer<UnversionedRowset>) invocation -> {
            Select select = invocation.getArgument(1);

            //jooq заменяет "pid IN (пустое_множество)" на "1 = 0"
            // если sql запрос должен вернуть пустой результат, то возвращаем emptyList()
            if (select.getSQL().contains("AND 1 = 0")) {
                return convertToGroupsRowset(emptyList());
            }

            return convertToGroupsRowset(List.of(groupInfo));
        }).when(gridYtSupport).selectRows(eq(groupInfo.getShard()), any(Select.class));

        adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(Set.of(groupInfo.getAdGroupId()))
                        .withCampaignIdIn(Set.of(groupInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));

        operatorInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN)
                        .getChiefUserInfo();
        context = ContextHelper.buildContext(operatorInfo.getUser(), productUserInfo.getUser());
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void getInternalAdGroupTargetingsDeserialization() {
        // Первый запрос получает свежую копию ответа
        var result = processor.processQuery(null, getQuery(), null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();

        var cacheKey = extractCacheKey(result.getData());
        assertThat(cacheKey).isNotNull();
        assertThat(cacheKey).isNotBlank();

        // Второй такой же запрос должен получить ответ из кэша
        // Для полноты эксперимента следует сбросить context тоже, чтобы "отчистить" все закешированные данные
        // от предыдущего запроса - мапы кампаний, групп и т.п.
        context = ContextHelper.buildContext(operatorInfo.getUser(), productUserInfo.getUser());
        gridContextProvider.setGridContext(context);
        adGroupsContainer.setCacheKey(cacheKey);
        result = processor.processQuery(null, getQuery(), null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
    }

    public static UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
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

    @SuppressWarnings("unchecked")
    private String extractCacheKey(Object data) {
        var clientData = ((Map<String, Object>) data).get("client");
        var adGroupsData = ((Map<String, Object>) clientData).get("adGroups");
        return (String) ((Map<String, Object>) adGroupsData).get("cacheKey");
    }

    private String getQuery() {
        return String.format(QUERY_TEMPLATE, context.getSubjectUser().getLogin(), graphQlSerialize(adGroupsContainer));
    }
}
