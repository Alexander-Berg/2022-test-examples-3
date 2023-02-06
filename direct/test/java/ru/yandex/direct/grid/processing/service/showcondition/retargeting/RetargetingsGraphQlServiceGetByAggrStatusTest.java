package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import one.util.streamex.EntryStream;
import org.jetbrains.annotations.Nullable;
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingBaseStatus;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderBy;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderByField;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
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
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_ADGROUPS;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_RETARGETINGS;
import static ru.yandex.direct.feature.FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.DIRECTPHRASESTATV2_BS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(Parameterized.class)
public class RetargetingsGraphQlServiceGetByAggrStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      retargetings(input: %s) {\n"
            + "          rowset {\n"
            + "            retargetingId\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
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
    private static final String ACTIVE_RETARGETING_AGGR_DATA = ""
            + "{\"r\": [\"ACTIVE\"], "
            + "\"s\": \"RUN_OK\"}";
    private static final String SUSPENDED_RETARGETING_AGGR_DATA = ""
            + "{\"r\": [\"SUSPENDED_BY_USER\"], " +
            "\"s\": \"STOP_OK\", " +
            "\"sts\": [\"SUSPENDED\"]}";

    private static final long REACH = 1L;
    private static final long SHOWS = 10L;
    private static final int LIMIT = 10;
    private static final int OFFSET = 0;
    // При запросе из YT возвращается 'clicks'. Поэтому для теста DIRECTPHRASESTATV2_BS.CLICKS == 'Clicks' не подходит
    private static final String CLICKS_FIELD_NAME = DIRECTPHRASESTATV2_BS.CLICKS.getName().toLowerCase();
    private static final String SHOWS_FIELD_NAME = DIRECTPHRASESTATV2_BS.SHOWS.getName().toLowerCase();

    private GridGraphQLContext context;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    private UserInfo userInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private RetargetingInfo retargetingInfo;

    @Parameterized.Parameter
    public FeatureName featureName;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SHOW_DNA_BY_DEFAULT},
                {HIDE_OLD_SHOW_CAMPS_FOR_DNA},
                {SHOW_AGGREGATED_STATUS_OPEN_BETA}
        });
    }

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        steps.featureSteps().addClientFeature(userInfo.getClientId(), featureName, true);

        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo(), CampaignsPlatform.SEARCH);

        retargetingInfo = steps.retargetingSteps().createDefaultRetargetingInActiveTextAdGroup(campaignInfo);
        adGroupInfo = retargetingInfo.getAdGroupInfo();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @After
    public void after() {
        aggregatedStatusesRepository.deleteAdGroupStatuses(userInfo.getShard(), singleton(adGroupInfo.getAdGroupId()));
    }

    @Test
    public void getRetargetings_WithEmptyFilterSetOfAggregateStatuses() {
        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA));

        ExecutionResult result = processQueryGetRetargetings(emptySet());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(retargetingInfo.getRetargetingId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getRetargetings_WithFilterByAggregateStatus() {
        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA));

        ExecutionResult result = processQueryGetRetargetings(Set.of(GdRetargetingBaseStatus.ACTIVE));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(retargetingInfo.getRetargetingId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getRetargetings_WhichDoesNotExistWithSentAggregateStatus() {
        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA));

        Set<GdRetargetingBaseStatus> filterByAggrStatus = Arrays.stream(GdRetargetingBaseStatus.values())
                .filter(status -> status != GdRetargetingBaseStatus.ACTIVE)
                .collect(Collectors.toSet());

        ExecutionResult result = processQueryGetRetargetings(filterByAggrStatus);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getAds_GetOnlyOneByStatusFrom2Retargetings() {
        var suspendedRetargetingInfo =
                steps.retargetingSteps().createDefaultRetargetingInActiveTextAdGroup(campaignInfo);
        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA,
                suspendedRetargetingInfo, SUSPENDED_RETARGETING_AGGR_DATA));

        Set<GdRetargetingBaseStatus> filterByAggrStatus = Arrays.stream(GdRetargetingBaseStatus.values())
                .filter(status -> status != GdRetargetingBaseStatus.ACTIVE)
                .collect(Collectors.toSet());

        ExecutionResult result = processQueryGetRetargetings(filterByAggrStatus);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(suspendedRetargetingInfo.getRetargetingId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getRetargetings_GetByFilterWithArchiveCampaign() {
        addAggrStatusesAdGroup(adGroupInfo.getAdGroupId(), ARCHIVE_ADGROUP_AGGR_DATA);

        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA));

        ExecutionResult result = processQueryGetRetargetings(Set.of(GdRetargetingBaseStatus.ACTIVE));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getRetargetings_GetWithoutFilterWithArchiveCampaign() {
        addAggrStatusesAdGroup(adGroupInfo.getAdGroupId(), ARCHIVE_ADGROUP_AGGR_DATA);

        mockYtResult(Map.of(retargetingInfo, ACTIVE_RETARGETING_AGGR_DATA));

        ExecutionResult result = processQueryGetRetargetings(null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(retargetingInfo.getRetargetingId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private void mockYtResult(Map<RetargetingInfo, String> retargetingInfoToAggrData) {
        doAnswer(invocation -> ytRetargetingsRowset(retargetingInfoToAggrData))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
    }

    private UnversionedRowset ytRetargetingsRowset(Map<RetargetingInfo, String> retargetingInfoToAggrData) {
        RowsetBuilder builder = rowsetBuilder();
        EntryStream.of(retargetingInfoToAggrData)
                .forKeyValue((retInfo, aggrData) -> {
                    addRetargetingAggrStatus(retInfo.getRetargetingId(), aggrData);
                    builder.add(rowBuilder()
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.CID, retInfo.getCampaignId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.PID, retInfo.getAdGroupId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_COND_ID, retInfo.getRetConditionId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_ID, retInfo.getRetargetingId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.AUTOBUDGET_PRIORITY,
                                    retInfo.getRetargeting().getAutobudgetPriority())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.IS_SUSPENDED,
                                    retInfo.getRetargeting().getIsSuspended())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.PRICE_CONTEXT,
                                    retInfo.getRetargeting().getPriceContext().longValue() * 1_000_000)
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.REACH, REACH)
                            .withColValue(SHOWS_FIELD_NAME, SHOWS)
                            .withColValue(CLICKS_FIELD_NAME, null));
                });
        return builder.build();
    }

    private void addRetargetingAggrStatus(Long retargetingId, String aggrData) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_RETARGETINGS, AGGR_STATUSES_RETARGETINGS.RET_ID,
                        AGGR_STATUSES_RETARGETINGS.AGGR_DATA, AGGR_STATUSES_RETARGETINGS.UPDATED,
                        AGGR_STATUSES_RETARGETINGS.IS_OBSOLETE)
                .values(retargetingId, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }

    private ExecutionResult processQueryGetRetargetings(Set<GdRetargetingBaseStatus> filterStatuses) {
        GdRetargetingsContainer container = new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                        .withOrder(Order.ASC)))
                .withLimitOffset(new GdLimitOffset().withOffset(OFFSET).withLimit(LIMIT))
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(LocalDate.now().minusDays(1))
                        .withTo(LocalDate.now()))
                .withFilter(new GdRetargetingFilter()
                        .withCampaignIdIn(Collections.singleton(campaignInfo.getCampaignId()))
                        .withAdGroupIdIn(Collections.singleton(adGroupInfo.getAdGroupId()))
                        .withStatusIn(filterStatuses));

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(container));
        return processor.processQuery(null, query, null, context);
    }

    private Map<String, Object> expectedData(Long retargetingId) {
        return Map.of(
                "client",
                Map.of("retargetings",
                        Map.of("rowset", expectedRowset(retargetingId))
                )
        );
    }

    private List<Object> expectedRowset(@Nullable Long retargetingId) {
        return retargetingId == null ? emptyList() : singletonList(Map.of("retargetingId", retargetingId));
    }

    private void addAggrStatusesAdGroup(Long pid, String aggrData) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.PID, AGGR_STATUSES_ADGROUPS.AGGR_DATA,
                        AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE)
                .values(pid, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }
}
