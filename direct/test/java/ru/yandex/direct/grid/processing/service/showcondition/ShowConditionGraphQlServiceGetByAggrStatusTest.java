package ru.yandex.direct.grid.processing.service.showcondition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jetbrains.annotations.Nullable;
import org.jooq.Select;
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

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsReason;
import ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsStatus;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionPrimaryStatus;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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
import static ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsReason.ACTIVE;
import static ru.yandex.direct.dbschema.ppc.enums.AggrStatusesKeywordsReason.DRAFT;
import static ru.yandex.direct.feature.FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.ORDER_BY_ID;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.getAnswer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на сервис, проверяем в основном то, что фильтры по аггрегированным статусам работают.
 */
@GridProcessingTest
@RunWith(Parameterized.class)
public class ShowConditionGraphQlServiceGetByAggrStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      showConditions(input: %s) {\n"
            + "          rowset {\n"
            + "            id\n"
            + "         }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

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

        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);

        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
    }

    @Test
    public void getKeywords_WithEmptyFilterSetOfAggregateStatuses() {
        doAnswer(getAnswer(List.of(adGroupInfo), List.of(keywordInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());
        addKeywordAggrStatus(keywordInfo.getId(), AggrStatusesKeywordsStatus.DRAFT, DRAFT);

        ExecutionResult result = processQueryGetKeywords(emptySet());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(keywordInfo.getId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getKeywords_WithFilterByAggregateStatus() {
        doAnswer(getAnswer(List.of(adGroupInfo), List.of(keywordInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());
        addKeywordAggrStatus(keywordInfo.getId(), AggrStatusesKeywordsStatus.DRAFT, DRAFT);

        ExecutionResult result = processQueryGetKeywords(Set.of(GdShowConditionPrimaryStatus.DRAFT));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(keywordInfo.getId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getKeywords_WhichDoesNotExistWithSentAggregateStatus() {
        doAnswer(getAnswer(List.of(adGroupInfo), List.of(keywordInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());
        addKeywordAggrStatus(keywordInfo.getId(), AggrStatusesKeywordsStatus.DRAFT, DRAFT);

        Set<GdShowConditionPrimaryStatus> filterByAggrStatus = Arrays.stream(GdShowConditionPrimaryStatus.values())
                .filter(status -> status != GdShowConditionPrimaryStatus.DRAFT)
                .collect(Collectors.toSet());

        ExecutionResult result = processQueryGetKeywords(filterByAggrStatus);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getKeywords_GetOnlyOneByStatusFrom2Keywords() {
        KeywordInfo activeKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        doAnswer(getAnswer(List.of(adGroupInfo), List.of(keywordInfo, activeKeywordInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());
        addKeywordAggrStatus(keywordInfo.getId(), AggrStatusesKeywordsStatus.DRAFT, DRAFT);
        addKeywordAggrStatus(activeKeywordInfo.getId(), AggrStatusesKeywordsStatus.RUN_OK, ACTIVE);

        ExecutionResult result = processQueryGetKeywords(Set.of(GdShowConditionPrimaryStatus.ACTIVE));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(activeKeywordInfo.getId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private ExecutionResult processQueryGetKeywords(Set<GdShowConditionPrimaryStatus> filterStatuses) {
        GdShowConditionsContainer container = getDefaultGdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter()
                        .withShowConditionIdIn(singleton(keywordInfo.getId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                        .withShowConditionStatusIn(filterStatuses))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));


        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(container));
        return processor.processQuery(null, query, null, context);
    }

    private void addKeywordAggrStatus(Long keywordId, AggrStatusesKeywordsStatus status,
                                      AggrStatusesKeywordsReason reason) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_KEYWORDS, AGGR_STATUSES_KEYWORDS.ID,
                        AGGR_STATUSES_KEYWORDS.STATUS, AGGR_STATUSES_KEYWORDS.REASON,
                        AGGR_STATUSES_KEYWORDS.UPDATED, AGGR_STATUSES_KEYWORDS.IS_OBSOLETE)
                .values(keywordId, status, reason, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }

    private Map<String, Object> expectedData(Long keywordId) {
        return Map.of(
                "client",
                Map.of("showConditions",
                        Map.of("rowset", expectedRowset(keywordId))
                )
        );
    }

    private List<Object> expectedRowset(@Nullable Long keywordId) {
        return keywordId == null ? emptyList() : singletonList(Map.of("id", keywordId));
    }
}
