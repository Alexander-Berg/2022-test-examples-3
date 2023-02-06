package ru.yandex.direct.grid.processing.service.showcondition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsStatus;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_KEYWORDS;
import static ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsReason.DRAFT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.ORDER_BY_ID;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.getAnswer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на отдачу aggr_statuses_keywords.is_obsolete параметра
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class ShowConditionGraphQlServiceGetAggrStatusIsObsoleteTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      showConditions(input: %s) {\n"
            + "          rowset {\n"
            + "            id\n"
            + "            aggregatedStatusInfo {\n"
            + "                isObsolete\n"
            + "            }"
            + "         }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private DslContextProvider dslContextProvider;

    private UserInfo userInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private KeywordInfo keywordInfo;
    private GridGraphQLContext context;

    public static Object[] parameters() {
        return new Object[][]{
                {true, ImmutableMap.of("isObsolete", true)},
                {false, ImmutableMap.of("isObsolete", false)},
        };
    }

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);

        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("is obsolete: {0}, expect: {1}")
    public void testGetIsObsolete(boolean isObsolete,
                                  Map<String, Object> mapIsObsoleteExpected) {
        doAnswer(getAnswer(List.of(adGroupInfo), List.of(keywordInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());
        createKeywordAggrStatus(keywordInfo.getId(), isObsolete);

        ExecutionResult result = processQueryGetKeywords();
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expect = expectResult(keywordInfo.getId(), mapIsObsoleteExpected);
        assertThat(data).is(matchedBy(beanDiffer(expect)));
    }

    private ExecutionResult processQueryGetKeywords() {
        GdShowConditionsContainer container = getDefaultGdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter()
                        .withShowConditionIdIn(singleton(keywordInfo.getId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId())))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(container));
        return processor.processQuery(null, query, null, context);
    }

    private void createKeywordAggrStatus(Long keywordId, boolean isObsolete) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_KEYWORDS, AGGR_STATUSES_KEYWORDS.ID,
                        AGGR_STATUSES_KEYWORDS.STATUS, AGGR_STATUSES_KEYWORDS.REASON,
                        AGGR_STATUSES_KEYWORDS.UPDATED, AGGR_STATUSES_KEYWORDS.IS_OBSOLETE)
                .values(keywordId, AggrStatusesKeywordsStatus.DRAFT, DRAFT, LocalDateTime.of(LocalDate.now(),
                        LocalTime.MIN), isObsolete ? 1L : 0L)
                .execute();
    }

    private Map<String, Object> expectResult(Long pid, Map<String, Object> mapToObsolete) {
        Map<String, Object> mapOfRowset = new HashMap<>();
        mapOfRowset.put("id", pid);
        mapOfRowset.put("aggregatedStatusInfo", mapToObsolete);
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "showConditions", ImmutableMap.of(
                                "rowset", singletonList(mapOfRowset)
                        )
                ));
    }
}
