package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
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

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.keyword.service.KeywordUtils;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.ShowConditionGraphQlService.ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.ORDER_BY_ID;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.getAnswer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на сервис, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class ShowConditionGraphQlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      showConditions(input: %s) {\n"
            + "          totalCount\n"
            + "          showConditionIds\n"
            + "          cacheKey\n"
            + "          filter {\n"
            + "            campaignIdIn\n"
            + "          }\n"
            + "          rowset {\n"
            + "            " + ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME + "\n"
            + "            ... on GdKeyword {\n"
            + "            price\n"
            + "            keyword\n"
            + "          }\n"
            + "          index\n"
            + "          id\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GdShowConditionsContainer showConditionsContainer;
    private KeywordInfo keywordInfoWithBanner;
    private KeywordInfo keywordInfoTwo;
    private KeywordInfo keywordInfoThree;
    private KeywordInfo keywordInfoFour;
    private CampaignInfo campaignInfo;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps groupSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private KeywordSteps keywordSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());

        campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        AdGroupInfo groupInfoWithBanner = groupSteps.createDefaultAdGroup(campaignInfo);
        bannerSteps.createDefaultBanner(groupInfoWithBanner);
        keywordInfoWithBanner = keywordSteps.createKeyword(groupInfoWithBanner);

        AdGroupInfo groupInfoTwo = groupSteps.createDefaultAdGroup(campaignInfo);
        keywordInfoTwo = keywordSteps.createKeyword(groupInfoTwo, defaultKeyword().withPrice(BigDecimal.TEN));
        keywordInfoThree = keywordSteps.createKeyword(groupInfoTwo, defaultKeyword().withPriceContext(BigDecimal.ONE));
        keywordInfoFour = keywordSteps.createKeyword(groupInfoTwo,
                defaultKeyword()
                        .withPhrase(KeywordUtils.AUTOTARGETING_PREFIX + defaultKeyword().getPhrase()));

        doAnswer(getAnswer(Arrays.asList(groupInfoWithBanner, groupInfoTwo),
                Arrays.asList(keywordInfoWithBanner, keywordInfoTwo, keywordInfoThree, keywordInfoFour)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        showConditionsContainer = getDefaultGdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter()
                        .withShowConditionIdIn(ImmutableSet.of(keywordInfoWithBanner.getId(), keywordInfoThree.getId(),
                                keywordInfoFour.getId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
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
    public void testService(boolean replaceFilterToFilterKey) {
        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(showConditionsContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(campaignInfo.getClientId(), jsonFilter);

            showConditionsContainer.setFilter(null);
            showConditionsContainer.setFilterKey(key);
        }

        ExecutionResult result = processQuery();

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "showConditions", ImmutableMap.of(
                                "totalCount", 4,
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("campaignIdIn", List.of(campaignInfo.getCampaignId()))
                                        .build(),
                                "rowset", Arrays.asList(
                                        ImmutableMap.builder()
                                                .put("index", 0)
                                                .put("id", keywordInfoWithBanner.getId())
                                                .put("price", getPrice(keywordInfoWithBanner))
                                                .put("keyword", keywordInfoWithBanner.getKeyword().getPhrase())
                                                .put(ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME, 1)
                                                .build(),
                                        ImmutableMap.builder()
                                                .put("index", 1)
                                                .put("id", keywordInfoTwo.getId())
                                                .put("price", getPrice(keywordInfoTwo))
                                                .put("keyword", keywordInfoTwo.getKeyword().getPhrase())
                                                .put(ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME, 0)
                                                .build(),
                                        ImmutableMap.builder()
                                                .put("index", 2)
                                                .put("id", keywordInfoThree.getId())
                                                .put("price", getPrice(keywordInfoThree))
                                                .put("keyword", keywordInfoThree.getKeyword().getPhrase())
                                                .put(ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME, 0)
                                                .build(),
                                        ImmutableMap.builder()
                                                .put("index", 3)
                                                .put("id", keywordInfoFour.getId())
                                                .put("price", getPrice(keywordInfoFour))
                                                .put("keyword", keywordInfoFour.getKeyword().getPhrase())
                                                .put(ADS_IN_AD_GROUP_COUNT_RESOLVER_NAME, 0)
                                                .build()
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client", "showConditions");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("showConditionIds"))
                .useMatcher(
                        containsInAnyOrder(keywordInfoWithBanner.getId(), keywordInfoTwo.getId(),
                                keywordInfoThree.getId(), keywordInfoFour.getId()));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }


    private ExecutionResult processQuery() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(showConditionsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private static BigDecimal getPrice(KeywordInfo keywordInfo) {
        return keywordInfo.getKeyword().getPrice()
                .setScale(2, RoundingMode.HALF_UP);
    }

}
