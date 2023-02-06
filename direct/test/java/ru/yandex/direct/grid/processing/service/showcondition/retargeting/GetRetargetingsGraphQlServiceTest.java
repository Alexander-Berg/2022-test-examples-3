package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingCondition;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionGoalsInfo;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionsAndAvailableShortcutsContainer;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionsContainer;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderBy;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderByField;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.RetargetingGraphQlService.RETARGETING_CONDITIONS_AND_AVAILABLE_SHORTCUTS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.showcondition.RetargetingGraphQlService.RETARGETING_CONDITIONS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.DIRECTPHRASESTATV2_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.RETARGETING_CONDITIONSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на метод сервиса, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class GetRetargetingsGraphQlServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String RETARGETING_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      retargetings(input: %s) {\n"
            + "          totalCount\n"
            + "          cacheKey\n"
            + "          filter {\n"
            + "            campaignIdIn\n"
            + "          }\n"
            + "          rowset {\n"
            + "            retargetingId\n"
            + "            retargetingConditionId\n"
            + "            campaignId\n"
            + "            adGroupId\n"
            + "            priceContext\n"
            + "            isSuspended\n"
            + "            reach\n"
            + "            autoBudgetPriority\n"
            + "            adGroup{\n"
            + "              id\n"
            + "            }\n"
            + "            stats {\n"
            + "              shows\n"
            + "              clicks\n"
            + "            }\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
    private static final String SEARCH_RETARGETING_FOR_EDIT_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      searchRetargetings(input: %s) {\n"
            + "          rowset {\n"
            + "            retargetingId\n"
            + "            retargetingConditionId\n"
            + "            priceContext\n"
            + "            adGroupId\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
    private static final String RETARGETING_WITH_CONDITION_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      retargetings(input: %s) {\n"
            + "          totalCount\n"
            + "          cacheKey\n"
            + "          rowset {\n"
            + "            retargetingId\n"
            + "            retargetingConditionId\n"
            + "            retargetingCondition {\n"
            + "              retargetingConditionId\n"
            + "              type\n"
            + "              name\n"
            + "              description\n"
            + "            }\n"
            + "            campaignId\n"
            + "            adGroupId\n"
            + "            priceContext\n"
            + "            isSuspended\n"
            + "            reach\n"
            + "            autoBudgetPriority\n"
            + "            adGroup{\n"
            + "              id\n"
            + "            }\n"
            + "            stats {\n"
            + "              shows\n"
            + "              clicks\n"
            + "            }\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
    private static final String RETARGETING_CONDITIONS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      %s(input: %s) {\n"
            + "          totalCount\n"
            + "          rowset {\n"
            + "            retargetingConditionId\n"
            + "            type\n"
            + "            name\n"
            + "            description\n"
            + "            availableForRetargeting\n"
            + "            interest\n"
            + "            goalsInfo {\n"
            + "               goalDomains\n"
            + "               hasGeoSegments\n"
            + "               hasUnavailableGoals\n"
            + "            }\n"
            + "            campaigns {\n"
            + "               id\n"
            + "               name\n"
            + "            }\n"
            + "          }\n"
            + "      }\n"
            + "  }\n"
            + "}\n";
    private static final long REACH = 1L;
    private static final long CLICKS = 0L;
    private static final long SHOWS = 10L;
    private static final int LIMIT = 10;
    private static final int OFFSET = 0;
    private static final String MEDIUM = "MEDIUM";
    // При запросе из YT возвращается 'clicks'. Поэтому для теста DIRECTPHRASESTATV2_BS.CLICKS == 'Clicks' не подходит
    private static final String CLICKS_FIELD_NAME = DIRECTPHRASESTATV2_BS.CLICKS.getName().toLowerCase();
    private static final String SHOWS_FIELD_NAME = DIRECTPHRASESTATV2_BS.SHOWS.getName().toLowerCase();

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps groupSteps;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private RetargetingSteps retargetingSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    private CampaignInfo campaignInfo;
    private AdGroupInfo defaultAdGroup;
    private RetargetingInfo retargetingInfo1;
    private RetargetingInfo retargetingInfo2;
    private RetargetingInfo shortcutRetargetingInfo;
    private AdGroupBidModifierInfo bidModifierInfo;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        var clientInfo = userInfo.getClientInfo();

        campaignInfo = campaignSteps.createActiveCampaign(clientInfo, CampaignsPlatform.SEARCH);
        defaultAdGroup = groupSteps.createDefaultAdGroup(campaignInfo);

        retargetingInfo1 = retargetingSteps.createDefaultRetargetingInActiveTextAdGroup(campaignInfo);
        retargetingInfo2 = retargetingSteps.createDefaultRetargetingInActiveTextAdGroup(campaignInfo);

        bidModifierInfo = steps.bidModifierSteps()
                .createAdGroupBidModifierRetargetingFilterWithRetCondIds(
                        defaultAdGroup,
                        List.of(retargetingInfo1.getRetConditionId()));

        featureSteps.addClientFeature(userInfo.getClientId(), FeatureName.SEARCH_RETARGETING_ENABLED, true);
        var goal = defaultGoalByType(GoalType.GOAL);
        var shortcutRetargetingConditionInfo = retConditionSteps.createDefaultRetCondition(List.of(goal), clientInfo,
                ConditionType.shortcuts, RuleType.OR);
        shortcutRetargetingInfo = retargetingSteps.createRetargeting(
                null, campaignInfo, shortcutRetargetingConditionInfo);
        doAnswer(getAnswerForRetargeting(Arrays.asList(retargetingInfo1, retargetingInfo2)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    public static Object[] parameters() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    @Test
    @TestCaseName("use filterKey instead filter: {0}")
    @Parameters(method = "parameters")
    public void checkRetargetings(boolean replaceFilterToFilterKey) {
        GdRetargetingFilter filter = new GdRetargetingFilter()
                .withCampaignIdIn(Collections.singleton(campaignInfo.getCampaignId()))
                .withAdGroupIdIn(Collections.singleton(defaultAdGroup.getAdGroupId()));
        LocalDate now = LocalDate.now();
        GdRetargetingsContainer container = new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                        .withOrder(Order.ASC)))
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(now.minusDays(1))
                        .withTo(now))
                .withLimitOffset(new GdLimitOffset().withOffset(OFFSET).withLimit(LIMIT))
                .withFilter(filter);

        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(container.getFilter());
            String key = filterShortcutsSteps.saveFilter(campaignInfo.getClientId(), jsonFilter);
            container.setFilter(null);
            container.setFilterKey(key);
        }

        String query = String.format(RETARGETING_QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "retargetings", ImmutableMap.of(
                                "totalCount", 2,
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("campaignIdIn", List.of(campaignInfo.getCampaignId()))
                                        .build(),
                                "rowset", Arrays.asList(
                                        retargetingToMap(retargetingInfo1, false),
                                        retargetingToMap(retargetingInfo2, false)
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client", "retargetings");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue());
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkRetargetingsWithCondition() {
        GdRetargetingFilter filter = new GdRetargetingFilter()
                .withCampaignIdIn(Collections.singleton(campaignInfo.getCampaignId()))
                .withAdGroupIdIn(Collections.singleton(defaultAdGroup.getAdGroupId()));
        LocalDate now = LocalDate.now();
        GdRetargetingsContainer container = new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                        .withOrder(Order.ASC)))
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(now.minusDays(1))
                        .withTo(now))
                .withLimitOffset(new GdLimitOffset().withOffset(OFFSET).withLimit(LIMIT))
                .withFilter(filter);
        doAnswer(getAnswerForRetargetingWithCondition(Arrays.asList(retargetingInfo1, retargetingInfo2)))
                .when(gridYtSupport).selectRows(eq(retargetingInfo1.getShard()), any(Select.class), anyBoolean());

        String query = String.format(RETARGETING_WITH_CONDITION_QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "retargetings", ImmutableMap.of(
                                "totalCount", 2,
                                "rowset", Arrays.asList(
                                        retargetingToMap(retargetingInfo1, true),
                                        retargetingToMap(retargetingInfo2, true)
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client", "retargetings");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue());
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkSearchRetargeting() {
        long expectedRetargetingId = ((BidModifierRetargetingFilter) bidModifierInfo.getBidModifier())
                .getRetargetingAdjustments().get(0)
                .getId();

        GdRetargetingFilter filter = new GdRetargetingFilter()
                .withCampaignIdIn(Collections.singleton(campaignInfo.getCampaignId()))
                .withAdGroupIdIn(Collections.singleton(defaultAdGroup.getAdGroupId()));
        GdRetargetingsContainer container = new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.GROUP_ID)
                        .withOrder(Order.DESC)))
                .withStatRequirements(new GdStatRequirements()
                        .withPreset(GdStatPreset.TODAY))
                .withLimitOffset(new GdLimitOffset().withOffset(OFFSET).withLimit(LIMIT))
                .withFilter(filter);

        String query = String.format(SEARCH_RETARGETING_FOR_EDIT_QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "searchRetargetings", ImmutableMap.of(
                                "rowset", Arrays.asList(
                                        Map.of(
                                                "adGroupId", defaultAdGroup.getAdGroupId(),
                                                "retargetingConditionId", retargetingInfo1.getRetConditionId(),
                                                "retargetingId", expectedRetargetingId,
                                                "priceContext", BigDecimal.ZERO
                                        )
                                )
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkRetargetingConditions() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                // даже будучи специально запрошенными, SHORTCUTS не должны возвращаться методом retargetingConditions
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.METRIKA_GOALS,
                        GdRetargetingConditionType.SHORTCUTS))
                .withInterest(false);
        GdRetargetingConditionsContainer container = new GdRetargetingConditionsContainer()
                .withFilter(filter);
        doAnswer(getAnswerForConditions(Arrays.asList(retargetingInfo1, retargetingInfo2, shortcutRetargetingInfo)))
                .when(gridYtSupport).selectRows(eq(retargetingInfo1.getShard()), any(Select.class));

        String query = String.format(RETARGETING_CONDITIONS_QUERY_TEMPLATE, context.getOperator().getLogin(),
                RETARGETING_CONDITIONS_RESOLVER_NAME, graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "retargetingConditions", ImmutableMap.of(
                                "totalCount", 2,
                                "rowset", Arrays.asList(
                                        retargetingConditionToMap(retargetingInfo1, campaignInfo),
                                        retargetingConditionToMap(retargetingInfo2, campaignInfo)
                                )
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkRetargetingConditionsAndAvailableShortcuts() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionTypeIn(
                        Set.of(GdRetargetingConditionType.METRIKA_GOALS, GdRetargetingConditionType.SHORTCUTS))
                .withInterest(false);
        var campaignId = campaignInfo.getCampaignId();
        GdRetargetingConditionsContainer container = new GdRetargetingConditionsAndAvailableShortcutsContainer()
                .withFilter(filter)
                .withCampaignId(campaignId);
        doAnswer(getAnswerForConditions(Arrays.asList(retargetingInfo1, retargetingInfo2, shortcutRetargetingInfo)))
                .when(gridYtSupport).selectRows(eq(retargetingInfo1.getShard()), any(Select.class));

        String query = String.format(RETARGETING_CONDITIONS_QUERY_TEMPLATE,
                context.getOperator().getLogin(), RETARGETING_CONDITIONS_AND_AVAILABLE_SHORTCUTS_RESOLVER_NAME,
                graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "retargetingConditionsAndAvailableShortcuts", ImmutableMap.of(
                                "totalCount", 3,
                                "rowset", Arrays.asList(
                                        retargetingConditionToMap(shortcutRetargetingInfo, campaignInfo),
                                        retargetingConditionToMap(retargetingInfo1, campaignInfo),
                                        retargetingConditionToMap(retargetingInfo2, campaignInfo)
                                )
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    private static ImmutableMap<Object, Object> retargetingToMap(RetargetingInfo retargetingInfo,
                                                                 boolean withRetargetingConditions) {
        ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder()
                .put("retargetingId", retargetingInfo.getRetargetingId())
                .put("retargetingConditionId", retargetingInfo.getRetConditionId())
                .put("campaignId", retargetingInfo.getCampaignId())
                .put("adGroupId", retargetingInfo.getAdGroupId())
                .put("priceContext",
                        retargetingInfo.getRetargeting().getPriceContext().setScale(2, RoundingMode.HALF_UP))
                .put("isSuspended", retargetingInfo.getRetargeting().getIsSuspended())
                .put("reach", REACH)
                .put("autoBudgetPriority", MEDIUM)
                .put("adGroup", ImmutableMap.builder()
                        .put("id", retargetingInfo.getAdGroupId())
                        .build())
                .put("stats", ImmutableMap.builder()
                        .put("shows", SHOWS)
                        .put("clicks", CLICKS)
                        .build());

        if (withRetargetingConditions) {
            RetargetingCondition condition = retargetingInfo.getRetConditionInfo().getRetCondition();
            builder.put("retargetingCondition", ImmutableMap.builder()
                    .put(GdRetargetingCondition.RETARGETING_CONDITION_ID.name(), condition.getId())
                    .put(GdRetargetingCondition.TYPE.name(), condition.getType().name().toUpperCase())
                    .put(GdRetargetingCondition.NAME.name(), condition.getName())
                    .put(GdRetargetingCondition.DESCRIPTION.name(), condition.getDescription())
                    .build());
        }

        return builder.build();
    }

    private static Map<Object, Object> retargetingConditionToMap(RetargetingInfo retargetingInfo,
                                                                 CampaignInfo campaignInfo) {
        RetargetingCondition condition = retargetingInfo.getRetConditionInfo().getRetCondition();

        return ImmutableMap.builder()
                .put(GdRetargetingCondition.RETARGETING_CONDITION_ID.name(), condition.getId())
                .put(GdRetargetingCondition.TYPE.name(), condition.getType().name().toUpperCase())
                .put(GdRetargetingCondition.NAME.name(), condition.getName())
                .put(GdRetargetingCondition.DESCRIPTION.name(), condition.getDescription())
                .put(GdRetargetingCondition.AVAILABLE_FOR_RETARGETING.name(), !condition.getNegative())
                .put(GdRetargetingCondition.INTEREST.name(), condition.getInterest())
                .put(GdRetargetingCondition.GOALS_INFO.name(), ImmutableMap.builder()
                        .put(GdRetargetingConditionGoalsInfo.GOAL_DOMAINS.name(), List.of())
                        .put(GdRetargetingConditionGoalsInfo.HAS_GEO_SEGMENTS.name(), false)
                        .put(GdRetargetingConditionGoalsInfo.HAS_UNAVAILABLE_GOALS.name(), false)
                        .build())
                .put(GdRetargetingCondition.CAMPAIGNS.name(), singletonList(ImmutableMap.builder()
                        .put(GdCampaignTruncated.ID.name(), campaignInfo.getCampaignId())
                        .put(GdCampaignTruncated.NAME.name(), campaignInfo.getCampaign().getName())
                        .build()))
                .build();
    }

    private static Answer<UnversionedRowset> getAnswerForRetargeting(List<RetargetingInfo> retargetings) {
        return invocation -> convertToRetargetingNode(retargetings, false);
    }

    private static Answer<UnversionedRowset> getAnswerForRetargetingWithCondition(List<RetargetingInfo> retargetings) {
        return invocation -> convertToRetargetingNode(retargetings, true);
    }

    private static Answer<UnversionedRowset> getAnswerForConditions(List<RetargetingInfo> retargetings) {
        List<RetargetingCondition> retargetingConditions = StreamEx.of(retargetings)
                .map(RetargetingInfo::getRetConditionInfo)
                .map(RetConditionInfo::getRetCondition)
                .toList();

        return invocation -> convertToRetargetingConditionNode(retargetingConditions);
    }

    private static UnversionedRowset convertToRetargetingNode(List<RetargetingInfo> retargetings,
                                                              boolean withRetargetingConditions) {
        RowsetBuilder builder = rowsetBuilder();
        retargetings.forEach(info -> {
                    RowBuilder rowBuilder = rowBuilder()
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.CID, info.getCampaignId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.PID, info.getAdGroupId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_COND_ID, info.getRetConditionId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.RET_ID, info.getRetargetingId())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.AUTOBUDGET_PRIORITY,
                                    info.getRetargeting().getAutobudgetPriority())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.IS_SUSPENDED,
                                    info.getRetargeting().getIsSuspended())
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.PRICE_CONTEXT,
                                    info.getRetargeting().getPriceContext().longValue() * 1_000_000)
                            .withColValue(BIDS_RETARGETINGTABLE_DIRECT.REACH, REACH)
                            .withColValue(SHOWS_FIELD_NAME, SHOWS)
                            .withColValue(CLICKS_FIELD_NAME, null);
                    if (withRetargetingConditions) {
                        RetargetingCondition condition = info.getRetConditionInfo().getRetCondition();
                        rowBuilder
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CLIENT_ID, condition.getClientId())
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RET_COND_ID, condition.getId())
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RETARGETING_CONDITIONS_TYPE,
                                        condition.getType().name())
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_NAME, condition.getName())
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_DESC,
                                        condition.getDescription())
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.IS_DELETED,
                                        booleanToLong(condition.getDeleted()))
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.NEGATIVE,
                                        booleanToLong(condition.getNegative()))
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.INTEREST,
                                        booleanToLong(condition.getInterest()))
                                .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_JSON,
                                        "[{\"goals\":[{\"goal_id\":4037315490,\"time\":60}],\"type\":\"or\"}]");
                    }
                    builder.add(rowBuilder);
                }
        );

        return builder.build();
    }

    private static UnversionedRowset convertToRetargetingConditionNode(List<RetargetingCondition> retargetingConditions) {
        RowsetBuilder builder = rowsetBuilder();
        retargetingConditions.forEach(condition -> builder.add(
                rowBuilder()
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CLIENT_ID, condition.getClientId())
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RET_COND_ID, condition.getId())
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RETARGETING_CONDITIONS_TYPE,
                                condition.getType().name())
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_NAME, condition.getName())
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_DESC, condition.getDescription())
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.IS_DELETED,
                                booleanToLong(condition.getDeleted()))
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.NEGATIVE,
                                booleanToLong(condition.getNegative()))
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.INTEREST,
                                booleanToLong(condition.getInterest()))
                        .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_JSON,
                                "[{\"goals\":[{\"goal_id\":4037315490,\"time\":60}],\"type\":\"or\"}]")
                )
        );

        return builder.build();
    }

}
