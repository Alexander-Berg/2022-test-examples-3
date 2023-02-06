package ru.yandex.direct.grid.processing.service.group;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import one.util.streamex.EntryStream;
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
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPrimaryStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesAdgroups.AGGR_STATUSES_ADGROUPS;
import static ru.yandex.direct.feature.FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQlServiceGetByAggrStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
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
    private static final String ACTIVE_ADGROUP_AGGR_DATA = ""
            + "{\n"
            + "    \"cnts\": {\n"
            + "        \"ads\": 3,\n"
            + "        \"b_s\": {\"RUN_OK\": 3},\n"
            + "        \"b_sts\": {\"PREACCEPTED\": 3},\n"
            + "        \"kw_s\": {\"ARCHIVED\": 34},\n"
            + "        \"kws\": 34,\n"
            + "        \"rets\": 0\n"
            + "    },\n"
            + "    \"r\": [\"ACTIVE\"],\n"
            + "    \"s\": \"RUN_OK\"\n"
            + "}";
    private static final String ARCHIVE_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ARCHIVED\"], "
            + "\"s\": \"ARCHIVED\", "
            + "\"sts\": [\"BS_RARELY_SERVED\"], "
            + "\"cnts\": {\"ads\": 1, "
            + "     \"b_s\": {\"ARCHIVED\": 1}, "
            + "     \"kws\": 1, "
            + "     \"kw_s\": {\"RUN_OK\": 1}, "
            + "     \"rets\": 0, "
            + "     \"b_sts\": {\"ARCHIVED\": 1, \"REJECTED\": 1, \"SUSPENDED\": 1}}"
            + "}";
    private static final String PROCESSING_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN\"], "
            + "\"s\": \"RUN_WARN\", "
            + "\"sts\": [\"BS_RARELY_SERVED\"], "
            + "\"cnts\": {\n"
            + "    \"ads\": 3,\n"
            + "    \"b_s\": {\"RUN_OK\": 3},\n"
            + "    \"b_sts\": {\"PREACCEPTED\": 3},\n"
            + "    \"kw_s\": {\"ARCHIVED\": 34},\n"
            + "    \"kws\": 34,\n"
            + "    \"rets\": 0\n"
            + "}}";


    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);

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

    public static Object[] parametersForGetByStatus() {
        return new Object[][]{
                // Все
                {"у группы ACTIVE статус, запрос без фильтра по статусам -> получаем adGroupId",
                        null, null, ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос без фильтра по статусам -> получаем adGroupId",
                        null, null, DRAFT_ADGROUP_AGGR_DATA, true},
                {"у группы PROCESSING статус, запрос без фильтра по статусам -> получаем adGroupId",
                        null, null, PROCESSING_ADGROUP_AGGR_DATA, true},
                {"у группы ARCHIVE статус, запрос без фильтра по статусам -> получаем adGroupId",
                        null, null, ARCHIVE_ADGROUP_AGGR_DATA, true},
                {"у группы нет статуса, запрос без фильтра по статусам -> получаем adGroupId",
                        null, null, null, true},
                // Все, кроме архивных
                {"у группы ACTIVE статус, запрос без фильтра по статусам и без archive -> получаем adGroupId",
                        null, false, ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос без фильтра по статусам и без archive -> получаем adGroupId",
                        null, false, DRAFT_ADGROUP_AGGR_DATA, true},
                {"у группы PROCESSING статус, запрос без фильтра по статусам и без archive -> получаем adGroupId",
                        null, false, PROCESSING_ADGROUP_AGGR_DATA, true},
                {"у группы ARCHIVE статус, запрос без фильтра по статусам и без archive -> adGroupId не получаем",
                        null, false, ARCHIVE_ADGROUP_AGGR_DATA, false},
                {"у группы нет статуса, запрос без фильтра по статусам и без archive -> получаем adGroupId",
                        null, false, null, true},
                // Только Архивные
                {"у группы ACTIVE статус, запрос без фильтра по статусам и с archive -> adGroupId не получаем",
                        null, true, ACTIVE_ADGROUP_AGGR_DATA, false},
                {"у группы DRAFT статус, запрос без фильтра по статусам и с archive -> adGroupId не получаем",
                        null, true, DRAFT_ADGROUP_AGGR_DATA, false},
                {"у группы PROCESSING статус, запрос без фильтра по статусам и с archive -> adGroupId не получаем",
                        null, true, PROCESSING_ADGROUP_AGGR_DATA, false},
                {"у группы ARCHIVE статус, запрос без фильтра по статусам и с archive -> получаем adGroupId",
                        null, true, ARCHIVE_ADGROUP_AGGR_DATA, true},
                {"у группы нет статуса, запрос без фильтра по статусам и с archive -> adGroupId не получаем",
                        null, true, null, false},
                // По статусам
                {"у группы ACTIVE статус, запрос с фильтром по ACTIVE статусам -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), null, ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос с фильтром по ACTIVE статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), null, DRAFT_ADGROUP_AGGR_DATA, false},
                {"у группы PROCESSING статус, запрос с фильтром по ACTIVE статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), null, PROCESSING_ADGROUP_AGGR_DATA, false},
                {"у группы ARCHIVE статус, запрос с фильтром по ACTIVE статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), null, ARCHIVE_ADGROUP_AGGR_DATA, false},
                {"у группы нет статуса, запрос с фильтром по ACTIVE статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), null, null, false},
                {"у группы нет статуса, запрос с фильтром по DRAFT статусам -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.DRAFT), null, null, true},
                // По статусам, без архивных
                {"у группы ACTIVE статус, запрос с фильтром по ACTIVE статусам и без archive -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), false, ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос с фильтром по ACTIVE статусам и без archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), false, DRAFT_ADGROUP_AGGR_DATA, false},
                {"у группы PROCESSING, запрос с фильтром по ACTIVE статусам и без archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), false, PROCESSING_ADGROUP_AGGR_DATA, false},
                {"у группы ARCHIVE статус, запрос с фильтром по ACTIVE статусам и без archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), false, ARCHIVE_ADGROUP_AGGR_DATA, false},
                {"у группы нет статуса, запрос с фильтром по ACTIVE статусам и без archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), false, null, false},
                // По статусам, с архивными
                {"у группы ACTIVE статус, запрос с фильтром по ACTIVE статусам и с archive -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), true, ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос с фильтром по ACTIVE статусам и с archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), true, DRAFT_ADGROUP_AGGR_DATA, false},
                {"у группы PROCESSING, запрос с фильтром по ACTIVE статусам и с archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), true, PROCESSING_ADGROUP_AGGR_DATA, false},
                {"у группы ARCHIVE статус, запрос с фильтром по ACTIVE статусам и с archive -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), true, ARCHIVE_ADGROUP_AGGR_DATA, true},
                {"у группы без статуса, запрос с фильтром по ACTIVE статусам и с archive -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE), true, null, false},
                // Несколько статусов в фильтре
                {"у группы ACTIVE статус, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE, GdAdGroupPrimaryStatus.DRAFT), null,
                        ACTIVE_ADGROUP_AGGR_DATA, true},
                {"у группы DRAFT статус, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE, GdAdGroupPrimaryStatus.DRAFT), null,
                        DRAFT_ADGROUP_AGGR_DATA, true},
                {"у группы PROCESSING статус, запрос с фильтром по ACTIVE,DRAFT статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE, GdAdGroupPrimaryStatus.DRAFT), null,
                        PROCESSING_ADGROUP_AGGR_DATA, false},
                {"у группы ARCHIVE статус, запрос с фильтром по ACTIVE,DRAFT статусам -> adGroupId не получаем",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE, GdAdGroupPrimaryStatus.DRAFT), null,
                        ARCHIVE_ADGROUP_AGGR_DATA, false},
                {"у группы нет статуса, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем adGroupId",
                        List.of(GdAdGroupPrimaryStatus.ACTIVE, GdAdGroupPrimaryStatus.DRAFT), null,
                        null, true},
                // Без статусов в фильтре
                {"у группы нет статуса, запрос с empty фильтром по статусам -> получаем adGroupId",
                        emptySet(), null, ACTIVE_ADGROUP_AGGR_DATA, true},
        };
    }

    @Test
    @Parameters(method = "parametersForGetByStatus")
    @TestCaseName("{0}")
    public void getAdGroups(@SuppressWarnings("unused") String testDescription,
                            Collection<GdAdGroupPrimaryStatus> filterStatuses,
                            Boolean withArchive,
                            String aggrData,
                            boolean getGroupIdInResult) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_AGGREGATED_STATUS_OPEN_BETA, true);

        Map<AdGroupInfo, String> groupInfoToAggrData = new HashMap<>();
        groupInfoToAggrData.put(groupInfo, aggrData);
        mockYtResult(groupInfoToAggrData);

        HashSet<GdAdGroupPrimaryStatus> filterStatusesSet =
                filterStatuses == null ? null : new HashSet<>(filterStatuses);
        ExecutionResult result = processQueryGetAdGroups(filterStatusesSet, withArchive);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(getGroupIdInResult ? adGroupId : null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    public static Object[] parametersForFeature() {
        return new Object[][]{
                {SHOW_DNA_BY_DEFAULT},
                {HIDE_OLD_SHOW_CAMPS_FOR_DNA},
                {SHOW_AGGREGATED_STATUS_OPEN_BETA},
        };
    }

    /**
     * Когда в запросе все статусы, кроме нашего
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAdGroups_WhichDoesNotExistWithSentAggregateStatus(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        mockYtResult(Map.of(groupInfo, DRAFT_ADGROUP_AGGR_DATA));

        Set<GdAdGroupPrimaryStatus> filterByAggrStatus = Arrays.stream(GdAdGroupPrimaryStatus.values())
                .filter(status -> status != GdAdGroupPrimaryStatus.DRAFT)
                .collect(Collectors.toSet());

        ExecutionResult result = processQueryGetAdGroups(filterByAggrStatus, true);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    /**
     * Когда есть две группы с разными статусами и запрашиваем только по одному статусу
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAdGroups_GetOnlyOneByStatusFrom2Groups(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(campaignId), clientInfo);
        steps.bannerSteps().createActiveTextBanner(adGroupInfo);

        mockYtResult(Map.of(groupInfo, DRAFT_ADGROUP_AGGR_DATA, adGroupInfo, ACTIVE_ADGROUP_AGGR_DATA));

        ExecutionResult result = processQueryGetAdGroups(Set.of(GdAdGroupPrimaryStatus.ACTIVE), null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(adGroupInfo.getAdGroupId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));

        aggregatedStatusesRepository
                .deleteAdGroupStatuses(clientInfo.getShard(), singleton(adGroupInfo.getAdGroupId()));
    }

    private ExecutionResult processQueryGetAdGroups(Set<GdAdGroupPrimaryStatus> filterStatuses,
                                                    @Nullable Boolean withArchived) {
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(singleton(campaignId))
                        .withPrimaryStatusIn(filterStatuses)
                        .withArchived(withArchived))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(Map<AdGroupInfo, String> groupInfoToAggrData) {
        doAnswer(invocation -> ytAdGroupRowset(groupInfoToAggrData))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
    }

    private UnversionedRowset ytAdGroupRowset(Map<AdGroupInfo, String> groupInfoToAggrData) {
        RowsetBuilder builder = rowsetBuilder();
        EntryStream.of(groupInfoToAggrData)
                .forKeyValue((groupInfo, aggrData) -> {
                    if (aggrData != null) {
                        addAggrStatusesAdGroup(groupInfo.getAdGroupId(), aggrData);
                    }
                    builder.add(rowBuilder()
                            .withColValue(PHRASESTABLE_DIRECT.PID.getName(), groupInfo.getAdGroupId())
                            .withColValue(PHRASESTABLE_DIRECT.CID.getName(), groupInfo.getCampaignId())
                            .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                    groupInfo.getAdGroupType().name().toLowerCase())
                            .withColValue(PHRASESTABLE_DIRECT.STATUS_MODERATE.getName(),
                                    groupInfo.getAdGroup().getStatusModerate().name().toLowerCase())
                            .withColValue(PHRASESTABLE_DIRECT.STATUS_POST_MODERATE.getName(),
                                    groupInfo.getAdGroup().getStatusPostModerate().name().toLowerCase()));
                });
        return builder.build();
    }

    private void addAggrStatusesAdGroup(Long pid, String aggrData) {
        dslContextProvider.ppc(clientInfo.getShard())
                .insertInto(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.PID, AGGR_STATUSES_ADGROUPS.AGGR_DATA,
                        AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE)
                .values(pid, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }

    private Map<String, Object> expectedData(Long agGroupId) {
        return Map.of(
                "client",
                Map.of("adGroups",
                        Map.of("rowset", expectedRowset(agGroupId))
                )
        );
    }

    private List<Object> expectedRowset(@Nullable Long agGroupId) {
        return agGroupId == null ? emptyList() : singletonList(Map.of("id", agGroupId));
    }
}
