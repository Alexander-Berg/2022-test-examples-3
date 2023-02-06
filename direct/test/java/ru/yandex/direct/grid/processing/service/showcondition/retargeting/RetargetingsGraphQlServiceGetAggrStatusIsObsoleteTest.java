package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderBy;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderByField;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_RETARGETINGS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на отдачу aggr_statuses_retargetings.is_obsolete параметра
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class RetargetingsGraphQlServiceGetAggrStatusIsObsoleteTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      retargetings(input: %s) {\n"
            + "          rowset {\n"
            + "            retargetingId\n"
            + "            aggregatedStatusInfo {\n"
            + "              isObsolete\n"
            + "            }"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
    private static final String ACTIVE_RETARGETING_AGGR_DATA = ""
            + "{\"r\": [\"ACTIVE\"], "
            + "\"s\": \"RUN_OK\"}";

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
    private DslContextProvider dslContextProvider;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    private GridGraphQLContext context;
    private UserInfo userInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo defaultAdGroup;
    private RetargetingInfo retargetingInfo;

    public static Object[] parameters() {
        return new Object[][]{
                {true, ImmutableMap.of("isObsolete", true)},
                {false, ImmutableMap.of("isObsolete", false)},
        };
    }

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        retargetingInfo = steps.retargetingSteps().createDefaultRetargetingInActiveTextAdGroup(campaignInfo);

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("is obsolete: {0}, expect: {1}")
    public void testGetIsObsolete(boolean isObsolete,
                                  Map<String, Object> mapIsObsoleteExpected) {
        mockYtResult(retargetingInfo);
        createRetargetingAggrStatus(retargetingInfo.getRetargetingId(), isObsolete);

        ExecutionResult result = processQueryGetRetargetings();
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expect = expectResult(retargetingInfo.getRetargetingId(), mapIsObsoleteExpected);
        assertThat(data).is(matchedBy(beanDiffer(expect)));
    }

    private ExecutionResult processQueryGetRetargetings() {
        GdRetargetingsContainer container = new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                        .withOrder(Order.ASC)))
                .withLimitOffset(new GdLimitOffset()
                        .withOffset(0)
                        .withLimit(10))
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(LocalDate.now().minusDays(1))
                        .withTo(LocalDate.now()))
                .withFilter(new GdRetargetingFilter()
                        .withCampaignIdIn(Collections.singleton(campaignInfo.getCampaignId()))
                        .withAdGroupIdIn(Collections.singleton(defaultAdGroup.getAdGroupId())));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(container));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(RetargetingInfo retInfo) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(rowBuilder()
                .withColValue(BIDS_RETARGETINGTABLE_DIRECT.CID, retInfo.getCampaignId())
                .withColValue(BIDS_RETARGETINGTABLE_DIRECT.PID, retInfo.getAdGroupId())
                .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_COND_ID, retInfo.getRetConditionId())
                .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_ID, retInfo.getRetargetingId()));
        doAnswer(invocation -> builder.build())
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
    }

    private void createRetargetingAggrStatus(Long retargetingId, boolean isObsolete) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_RETARGETINGS, AGGR_STATUSES_RETARGETINGS.RET_ID,
                        AGGR_STATUSES_RETARGETINGS.AGGR_DATA, AGGR_STATUSES_RETARGETINGS.UPDATED,
                        AGGR_STATUSES_RETARGETINGS.IS_OBSOLETE)
                .values(retargetingId, ACTIVE_RETARGETING_AGGR_DATA, LocalDateTime.of(LocalDate.now(), LocalTime.MIN),
                        isObsolete ? 1L : 0L)
                .execute();
    }

    private Map<String, Object> expectResult(Long pid, Map<String, Object> mapToObsolete) {
        Map<String, Object> mapOfRowset = new HashMap<>();
        mapOfRowset.put("retargetingId", pid);
        mapOfRowset.put("aggregatedStatusInfo", mapToObsolete);
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "retargetings", ImmutableMap.of(
                                "rowset", singletonList(mapOfRowset)
                        )
                ));
    }
}
