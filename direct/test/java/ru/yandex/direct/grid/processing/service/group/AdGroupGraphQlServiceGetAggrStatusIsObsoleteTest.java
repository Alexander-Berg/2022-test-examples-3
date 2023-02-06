package ru.yandex.direct.grid.processing.service.group;

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
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
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
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_ADGROUPS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на отдачу aggr_statuses_adgroups.is_obsolete параметра
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQlServiceGetAggrStatusIsObsoleteTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        aggregatedStatusInfo {\n"
            + "          isObsolete\n"
            + "        }"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String DRAFT_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING\"], "
            + "\"s\": \"DRAFT\", "
            + "\"sts\": [\"DRAFT\"], "
            + "\"cnts\": {"
            + "  \"ads\": 0, "
            + "  \"kws\": 0,"
            + "  \"rets\": 0"
            + "}}";

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
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    private GridGraphQLContext context;
    private AdGroupInfo groupInfo;
    private ClientInfo clientInfo;
    private long campaignId;
    private long adGroupId;

    public static Object[] parameters() {
        return new Object[][]{
                {true, ImmutableMap.of("isObsolete", true)},
                {false, ImmutableMap.of("isObsolete", false)},
        };
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        groupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(campaignId), clientInfo);
        adGroupId = groupInfo.getAdGroupId();

        User operator = clientInfo.getChiefUserInfo().getUser();
        context = ContextHelper.buildContext(operator);
        gridContextProvider.setGridContext(context);
    }

    @After
    public void after() {
        aggregatedStatusesRepository.deleteAdGroupStatuses(clientInfo.getShard(), singleton(adGroupId));
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("is obsolete: {0}, expect: {1}")
    public void testGetIsObsolete(boolean isObsolete,
                                  Map<String, Object> mapIsObsoleteExpected) {
        createAggrStatusesAdGroup(adGroupId, isObsolete);
        mockYtResult(groupInfo);

        ExecutionResult result = processQueryGetAdGroups();
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expect = expectResult(adGroupId, mapIsObsoleteExpected);
        assertThat(data).is(matchedBy(beanDiffer(expect)));
    }

    private ExecutionResult processQueryGetAdGroups() {
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(singleton(campaignId)))
                .withOrderBy(Collections.singletonList(new GdAdGroupOrderBy()
                        .withField(GdAdGroupOrderByField.ID)
                        .withOrder(Order.ASC)));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(AdGroupInfo adGroupInfo) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(rowBuilder()
                .withColValue(PHRASESTABLE_DIRECT.PID.getName(), adGroupInfo.getAdGroupId())
                .withColValue(PHRASESTABLE_DIRECT.CID.getName(), adGroupInfo.getCampaignId())
                .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                        adGroupInfo.getAdGroupType().name().toLowerCase()));
        doReturn(builder.build())
                .when(gridYtSupport).selectRows(eq(adGroupInfo.getShard()), any(Select.class), anyBoolean());
    }

    private void createAggrStatusesAdGroup(Long pid, boolean isObsolete) {
        dslContextProvider.ppc(clientInfo.getShard())
                .insertInto(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.PID, AGGR_STATUSES_ADGROUPS.AGGR_DATA,
                        AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE)
                .values(pid, DRAFT_ADGROUP_AGGR_DATA, LocalDateTime.of(LocalDate.now(), LocalTime.MIN),
                        isObsolete ? 1L : 0L)
                .execute();
    }

    private Map<String, Object> expectResult(Long pid, Map<String, Object> mapToObsolete) {
        Map<String, Object> mapOfRowset = new HashMap<>();
        mapOfRowset.put("id", pid);
        mapOfRowset.put("aggregatedStatusInfo", mapToObsolete);
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "adGroups", ImmutableMap.of(
                                "rowset", Collections.singletonList(mapOfRowset)
                        )
                ));
    }
}
