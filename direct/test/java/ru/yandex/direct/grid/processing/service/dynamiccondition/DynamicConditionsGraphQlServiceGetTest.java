package ru.yandex.direct.grid.processing.service.dynamiccondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetPrimaryStatus;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicFeedConditionOperator;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicWebpageConditionOperand;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicWebpageConditionOperator;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRules;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class DynamicConditionsGraphQlServiceGetTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    dynamicAdTargets(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        dynamicConditionId\n"
            + "        adGroupId\n"
            + "        campaignId\n"
            + "        name\n"
            + "        price\n"
            + "        priceContext\n"
            + "        autobudgetPriority\n"
            + "        isSuspended\n"
            + "        tab\n"
            + "        ... on GdDynamicFeedAdTarget {\n"
            + "          feedConditions {\n"
            + "            field\n"
            + "            operator\n"
            + "            stringValue\n"
            + "          }\n"
            + "        }\n"
            + "        ... on GdDynamicWebpageAdTarget {\n"
            + "          webpageConditions {\n"
            + "            operand\n"
            + "            operator\n"
            + "            arguments\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "      filter {\n"
            + "        campaignIdIn\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    private AdGroupInfo dynamicFeedAdGroup;
    private AdGroupInfo dynamicTextAdGroup;
    private GridGraphQLContext context;

    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);
        dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
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
    public void getDynamicFeedAdTargets(boolean replaceFilterToFilterKey) {
        DynamicFeedAdTarget dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup);

        GdDynamicAdTargetsContainer gdDynamicAdTargetsContainer =
                new GdDynamicAdTargetsContainer()
                        .withFilter(new GdDynamicAdTargetFilter()
                                .withCampaignIdIn(ImmutableSet.of(dynamicFeedAdGroup.getCampaignId())));

        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(gdDynamicAdTargetsContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(dynamicFeedAdGroup.getClientId(), jsonFilter);
            gdDynamicAdTargetsContainer.setFilter(null);
            gdDynamicAdTargetsContainer.setFilterKey(key);
        }

        ExecutionResult result = processQuery(gdDynamicAdTargetsContainer, QUERY_TEMPLATE);
        Map<String, Object> data = result.getData();

        Map<String, Object> expectedData = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "dynamicAdTargets", ImmutableMap.of(
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("campaignIdIn", List.of(dynamicFeedAdGroup.getCampaignId()))
                                        .build(),
                                "rowset", singletonList(map(
                                        "id", dynamicFeedAdTarget.getId(),
                                        "dynamicConditionId", dynamicFeedAdTarget.getDynamicConditionId(),
                                        "adGroupId", dynamicFeedAdGroup.getAdGroupId(),
                                        "campaignId", dynamicFeedAdGroup.getCampaignId(),
                                        "name", dynamicFeedAdTarget.getConditionName(),
                                        "price", roundPrice(dynamicFeedAdTarget.getPrice()),
                                        "priceContext", roundPrice(dynamicFeedAdTarget.getPriceContext()),
                                        "autobudgetPriority", null,
                                        "isSuspended", dynamicFeedAdTarget.getIsSuspended(),
                                        "tab", dynamicFeedAdTarget.getTab().name(),
                                        "feedConditions", singletonList(ImmutableMap.of(
                                                "field", "categoryId",
                                                "operator", GdDynamicFeedConditionOperator.EQUALS_ANY.name(),
                                                "stringValue", "[\"1\",\"2\"]"))
                                ))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expectedData).useCompareStrategy(allFields())));
    }

    @Test
    public void getDynamicWebpageAdTargets_whenAllPageCondition() {
        List<WebpageRule> rules = singletonList(new WebpageRule().withType(WebpageRuleType.ANY));
        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTargetWithRules(dynamicTextAdGroup, rules);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicTextAdTarget);

        GdDynamicAdTargetsContainer gdDynamicAdTargetsContainer =
                new GdDynamicAdTargetsContainer()
                        .withFilter(new GdDynamicAdTargetFilter()
                                .withCampaignIdIn(ImmutableSet.of(dynamicTextAdGroup.getCampaignId())));

        ExecutionResult result = processQuery(gdDynamicAdTargetsContainer, QUERY_TEMPLATE);
        Map<String, Object> data = result.getData();

        Map<String, Object> expectedData = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "dynamicAdTargets", ImmutableMap.of(
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("campaignIdIn", List.of(dynamicTextAdGroup.getCampaignId()))
                                        .build(),
                                "rowset", singletonList(map(
                                        "id", dynamicTextAdTarget.getId(),
                                        "dynamicConditionId", dynamicTextAdTarget.getDynamicConditionId(),
                                        "adGroupId", dynamicTextAdGroup.getAdGroupId(),
                                        "campaignId", dynamicTextAdGroup.getCampaignId(),
                                        "name", dynamicTextAdTarget.getConditionName(),
                                        "price", roundPrice(dynamicTextAdTarget.getPrice()),
                                        "priceContext", roundPrice(dynamicTextAdTarget.getPriceContext()),
                                        "autobudgetPriority", GdShowConditionAutobudgetPriority.MEDIUM.name(),
                                        "isSuspended", dynamicTextAdTarget.getIsSuspended(),
                                        "tab", dynamicTextAdTarget.getTab().name(),
                                        "webpageConditions", emptyList()
                                ))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expectedData).useCompareStrategy(allFields())));
    }

    @Test
    public void getDynamicWebpageAdTargets_whenNotAllPageCondition() {
        List<WebpageRule> rules = singletonList(new WebpageRule()
                .withType(WebpageRuleType.URL)
                .withKind(WebpageRuleKind.EXACT)
                .withValue(singletonList("test/")));

        DynamicTextAdTarget dynamicTextAdTarget = defaultDynamicTextAdTargetWithRules(dynamicTextAdGroup, rules);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicTextAdTarget);

        String template = ""
                + "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    dynamicAdTargets(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "        ... on GdDynamicWebpageAdTarget {\n"
                + "          webpageConditions {\n"
                + "            operand\n"
                + "            operator\n"
                + "            arguments\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        GdDynamicAdTargetsContainer gdDynamicAdTargetsContainer =
                new GdDynamicAdTargetsContainer()
                        .withFilter(new GdDynamicAdTargetFilter()
                                .withCampaignIdIn(ImmutableSet.of(dynamicTextAdGroup.getCampaignId())));

        ExecutionResult result = processQuery(gdDynamicAdTargetsContainer, template);
        Map<String, Object> data = result.getData();

        Map<String, Object> expectedData = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "dynamicAdTargets", ImmutableMap.of(
                                "rowset", singletonList(map(
                                        "id", dynamicTextAdTarget.getId(),
                                        "webpageConditions", singletonList(ImmutableMap.of(
                                                "operand", GdDynamicWebpageConditionOperand.URL.name(),
                                                "operator", GdDynamicWebpageConditionOperator.CONTAINS_ANY.name(),
                                                "arguments", singletonList("test/")))
                                ))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expectedData).useCompareStrategy(allFields())));
    }

    @Test
    public void getDynamicAdTargetStatuses() {
        DynamicTextAdTarget activeDynamicAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .withIsSuspended(false);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, activeDynamicAdTarget);

        DynamicTextAdTarget suspendedDynamicAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .withIsSuspended(true);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, suspendedDynamicAdTarget);

        String templateWithStatus = ""
                + "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    dynamicAdTargets(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "        status {\n"
                + "          primaryStatus\n"
                + "          suspended\n"
                + "          readOnly\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        GdDynamicAdTargetsContainer gdDynamicAdTargetsContainer =
                new GdDynamicAdTargetsContainer()
                        // TODO: orderBy
                        .withFilter(new GdDynamicAdTargetFilter()
                                .withCampaignIdIn(ImmutableSet.of(dynamicTextAdGroup.getCampaignId())));

        ExecutionResult result = processQuery(gdDynamicAdTargetsContainer, templateWithStatus);
        Map<String, Object> data = result.getData();

        Map<String, Object> expectedData = map(
                "client", map(
                        "dynamicAdTargets", map(
                                "rowset", list(
                                        map("id", activeDynamicAdTarget.getId(),
                                                "status", map(
                                                        "primaryStatus", GdDynamicAdTargetPrimaryStatus.ACTIVE.name(),
                                                        "suspended", false,
                                                        "readOnly", false)),
                                        map("id", suspendedDynamicAdTarget.getId(),
                                                "status", map(
                                                        "primaryStatus", GdDynamicAdTargetPrimaryStatus.STOPPED.name(),
                                                        "suspended", true,
                                                        "readOnly", false))
                                )
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expectedData).useCompareStrategy(allFields())));
    }

    private ExecutionResult processQuery(GdDynamicAdTargetsContainer gdDynamicAdTargetsContainer, String template) {
        String query = String.format(template, context.getOperator().getLogin(),
                graphQlSerialize(gdDynamicAdTargetsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));
        return result;
    }

    private static BigDecimal roundPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
