package ru.yandex.direct.grid.processing.service.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.EntryStream;
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
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.adgroup.generation.AdGroupKeywordRecommendationService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.group.container.GdiAdGroupRegionsInfo;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContext;
import ru.yandex.direct.grid.processing.model.group.GdMinusKeywordsPackInfo;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRelevanceMatchCategory;
import ru.yandex.direct.grid.processing.model.showcondition.GdAdGroupGetKeywordRecommendationInput;
import ru.yandex.direct.grid.processing.model.showcondition.GdKeywordsByCategory;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.utils.Counter;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.utils.ListUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.client.ClientGraphQlService.CLIENT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.ADS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.AD_GROUPS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_ADS_CALLOUTS_MODERATION_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_ADS_CALLOUTS_MODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_MODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_BS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_DELETED_AD_GROUP_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_REMODERATE_ADS_CALLOUTS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_REMODERATE_ADS_CALLOUTS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.KEYWORDS_BY_CATEGORY_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.KEYWORDS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.MAIN_AD_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на сервис, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      totalCount\n"
            + "      adGroupIds\n"
            + "      cacheKey\n"
            + "      filter {\n"
            + "        campaignIdIn\n"
            + "      }\n"
            + "      rowset {\n"
            + "        index\n"
            + "        id\n"
            + "        minusKeywords\n"
            + "        libraryMinusKeywordsPacks {\n"
            + "          id\n"
            + "          name\n"
            + "        }\n"
            + "        regionsInfo {\n"
            + "          regionIds\n"
            + "        }\n"
            + "        campaign {\n"
            + "          id\n"
            + "          isRecommendationsManagementEnabled"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String ADS_COUNT_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        " + ADS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String KEYWORDS_COUNT_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        " + KEYWORDS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";


    private static final String CAN_DELETE_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      features {\n"
            + "         " + CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + CAN_DELETED_AD_GROUP_RESOLVER_NAME + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      features {\n"
            + "         " + CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + CAN_BE_SENT_TO_BS_RESOLVER_NAME + "\n"
            + "          " + CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME + "\n"
            + "          " + CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME + "\n"
            + "          " + CAN_ACCEPT_MODERATION_RESOLVER_NAME + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String CAN_ACCEPT_AND_REMODERATE_ADS_CALLOUTS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      features {\n"
            + "         " + CAN_REMODERATE_ADS_CALLOUTS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_ACCEPT_ADS_CALLOUTS_MODERATION_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + CAN_REMODERATE_ADS_CALLOUTS_RESOLVER_NAME + "\n"
            + "          " + CAN_ACCEPT_ADS_CALLOUTS_MODERATION_RESOLVER_NAME + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String KEYWORD_RECOMMENDATION_BY_CATEGORY_TEMPLATE = ""
            + "{" + KEYWORDS_BY_CATEGORY_NAME + "\n"
            + "  (input: %s) {\n"
            + "    keywordByCategory\n"
            + "  }\n"
            + "}";


//    query {
//  keywordsByCategory(
//    input:{
//      adGroupId:3285873386
//      keywordRecommendationData: {
//        regionIds:[10002]
//        minusKeywords:[]
//        keywords:[]
//      }
//    }
//  ) {
//    keywordByCategory
//    validationResult {
//      errors {
//        code
//      }
//    }
//  }
//}

    private static final String KEYWORDS_BY_CATEGORY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        access {\n"
            + "            " + KEYWORDS_BY_CATEGORY_NAME + "{\n"
            + "                 keywordByCategory" + "\n"
            + "          }\n"
            + "         }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);

    private UserInfo userInfo;
    private Map<Long, MinusKeywordsPack> existingMinusKeywordsPacks = new HashMap<>();
    private GdAdGroupsContainer adGroupsContainer;
    private GridGraphQLContext context;
    private AdGroupInfo groupInfoOne;
    private AdGroupInfo groupInfoThree;
    private CampaignInfo campaignInfo;

    private List<AdGroupInfo> groupInfos;

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
    private BannerSteps bannerSteps;

    @Autowired
    private KeywordSteps keywordSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private AdGroupKeywordRecommendationService keywordGenerationService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    @Before
    public void initTestData() {
        userInfo = userSteps.createDefaultUser();

        campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());

        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(campaignInfo.getClientInfo());
        existingMinusKeywordsPacks
                .put(libraryMinusKeywordsPack.getMinusKeywordPackId(), libraryMinusKeywordsPack.getMinusKeywordsPack());
        groupInfoOne = groupSteps.createAdGroup(TestGroups.activeTextAdGroup()
                        .withGeo(asList(Region.MOSCOW_REGION_ID, Region.SAINT_PETERSBURG_REGION_ID))
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                campaignInfo);
        //noinspection unused
        AdGroupInfo groupInfoTwo = groupSteps.createDefaultAdGroup(campaignInfo);

        CampaignInfo campaignInfoTwo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());
        //noinspection unused
        AdGroup adGroup = TestGroups.defaultTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(Collections.singletonList("minussssss"));
        groupInfoThree = groupSteps.createAdGroup(TestGroups.activeTextAdGroup()
                        .withGeo(asList(Region.MOSCOW_REGION_ID, Region.SAINT_PETERSBURG_REGION_ID))
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                campaignInfoTwo);

        groupInfos = new ArrayList<>(asList(groupInfoOne, groupInfoThree));
        doAnswer((Answer<UnversionedRowset>) invocation -> {
            Select select = invocation.getArgument(1);

            //jooq заменяет "pid IN (пустое_множество)" на "1 = 0"
            // если sql запрос должен вернуть пустой результат, то возвращаем emptyList()
            if (select.getSQL().contains("AND 1 = 0")) {
                return convertToGroupsRowset(emptyList());
            }

            return convertToGroupsRowset(groupInfos);
        }).when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(ImmutableSet.of(groupInfoOne.getAdGroupId(), groupInfoThree.getAdGroupId()))
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
            String jsonFilter = JsonUtils.toJson(adGroupsContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(groupInfoOne.getClientId(), jsonFilter);

            adGroupsContainer.setFilter(null);
            adGroupsContainer.setFilterKey(key);
        }

        ExecutionResult result = processQuery(QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        List<Map> expectedRowset = getExpectedRowset(groupInfoOne, groupInfoThree);
        Map<String, Object> expected = singletonMap(
                "client",
                ImmutableMap.of("adGroups", ImmutableMap.of(
                        "totalCount", 2,
                        "filter", ImmutableMap.<String, Object>builder()
                                .put("campaignIdIn", List.of(campaignInfo.getCampaignId()))
                                .build(),
                        "rowset", expectedRowset)
                )
        );

        BeanFieldPath prefix = newPath("client", "adGroups");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("adGroupIds"))
                .useMatcher(containsInAnyOrder(groupInfoOne.getAdGroupId(), groupInfoThree.getAdGroupId()));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void testAdsCountResolver() {
        AdGroupInfo groupInfoWithBanner = groupInfoThree;
        bannerSteps.createDefaultBanner(groupInfoWithBanner);
        AdGroupInfo groupInfoPerformance = groupSteps.createDefaultPerformanceAdGroup(userInfo.getClientInfo());
        groupInfos.add(groupInfoPerformance);
        steps.performanceMainBannerSteps().createPerformanceMainBanner(groupInfoPerformance);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(ImmutableSet.of(groupInfoOne.getCampaignId(), groupInfoWithBanner.getCampaignId(),
                        groupInfoPerformance.getCampaignId()))
                .withAdGroupIdIn(ImmutableSet.of(groupInfoOne.getAdGroupId(), groupInfoWithBanner.getAdGroupId(),
                        groupInfoPerformance.getAdGroupId()));
        ExecutionResult result = processQuery(ADS_COUNT_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map<String, Integer> expectedAdsCountForGroupOne = singletonMap(ADS_COUNT_RESOLVER_NAME, 0);
        Map<String, Integer> expectedAdsCountForGroupWithBanner = singletonMap(ADS_COUNT_RESOLVER_NAME, 1);
        Map<String, Integer> expectedAdsCountForGroupPerformance = singletonMap(ADS_COUNT_RESOLVER_NAME, 0);
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.ROWSET.name(),
                                Arrays.asList(expectedAdsCountForGroupOne, expectedAdsCountForGroupWithBanner,
                                        expectedAdsCountForGroupPerformance))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testKeywordsCountResolver() {
        var groupInfoWithKeyword = groupInfoThree;
        keywordSteps.createKeywordWithText("stub", groupInfoWithKeyword);
        keywordSteps.createKeywordWithText("stub1", groupInfoOne);
        keywordSteps.createKeywordWithText("stub2", groupInfoOne);
        campaignSteps.archiveCampaign(groupInfoOne.getCampaignInfo());
        adGroupsContainer.getFilter()
                .withCampaignIdIn(Set.of(groupInfoOne.getCampaignId(), groupInfoWithKeyword.getCampaignId()))
                .withAdGroupIdIn(Set.of(groupInfoOne.getAdGroupId(), groupInfoWithKeyword.getAdGroupId()));

        var executionResult = processQuery(KEYWORDS_COUNT_QUERY_TEMPLATE);
        GraphQLUtils.logErrors(executionResult.getErrors());
        assertThat(executionResult.getErrors()).isEmpty();

        Map<String, Object> data = executionResult.getData();
        var expectedKeywordsCountForGroupOne = Map.of(KEYWORDS_COUNT_RESOLVER_NAME, 2);
        var expectedKeywordsCountForGroupWithKeyword = Map.of(KEYWORDS_COUNT_RESOLVER_NAME, 1);
        var expected = Map.of(CLIENT_RESOLVER_NAME,
                Map.of(AD_GROUPS_RESOLVER_NAME,
                        Map.of(GdAdGroupsContext.ROWSET.name(),
                                List.of(expectedKeywordsCountForGroupOne, expectedKeywordsCountForGroupWithKeyword))));

        assertThat(data).is(matchedBy((beanDiffer(expected))));
    }

    @Test
    public void testKeywordsByCategoryResolver() {

        var keywordByCategory = Map.of(RelevanceMatchCategory.exact_mark, List.of("test", "test2"));
        var input = new GdAdGroupGetKeywordRecommendationInput()
                .withAdGroupId(groupInfoOne.getAdGroupId());

        doReturn(Result.successful(keywordByCategory))
                .when(keywordGenerationService).recommendedKeywords(any(ClientId.class), any());

        var expectedKeywordByCategory =
                EntryStream.of(keywordByCategory).mapKeys(k -> GdRelevanceMatchCategory.fromTypedValue(k.name()))
                        .toMap();
        var executionResult = processQuery(KEYWORD_RECOMMENDATION_BY_CATEGORY_TEMPLATE, input);
        GraphQLUtils.logErrors(executionResult.getErrors());
        assertThat(executionResult.getErrors()).isEmpty();

        Map<String, Object> data = executionResult.getData();
        var expected =
                Map.of(KEYWORDS_BY_CATEGORY_NAME, Map.of(GdKeywordsByCategory.KEYWORD_BY_CATEGORY.name(),
                        expectedKeywordByCategory));

        assertThat(data).is(matchedBy((beanDiffer(expected))));
    }

    @Test
    public void testMainAdResolver() {
        AdGroupInfo groupInfoWithBanner = groupInfoThree;
        TextBannerInfo banner = bannerSteps.createDefaultBanner(groupInfoWithBanner);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(ImmutableSet.of(groupInfoOne.getCampaignId(), groupInfoWithBanner.getCampaignId()))
                .withAdGroupIdIn(ImmutableSet.of(groupInfoOne.getAdGroupId(), groupInfoWithBanner.getAdGroupId()));
        ExecutionResult result = processQuery(""
                + "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    adGroups(input: %s) {\n"
                + "      rowset {\n"
                + "        " + MAIN_AD_RESOLVER_NAME + " {\n"
                + "          id\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n");

        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();

        Map expectedMainAdForGroupOne = singletonMap(MAIN_AD_RESOLVER_NAME, null);
        Map expectedMainAdForGroupWithBanner =
                singletonMap(MAIN_AD_RESOLVER_NAME, singletonMap("id", banner.getBannerId()));
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.ROWSET.name(),
                                Arrays.asList(expectedMainAdForGroupOne, expectedMainAdForGroupWithBanner))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCanDeleteAdGroupsResolvers() {
        //добавляем активный баннер в группу, чтобы нельзя было удалить группу
        bannerSteps.createActiveTextBanner(groupInfoThree);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(ImmutableSet.of(groupInfoOne.getCampaignId(), groupInfoThree.getCampaignId()))
                .withAdGroupIdIn(ImmutableSet.of(groupInfoOne.getAdGroupId(), groupInfoThree.getAdGroupId()));
        ExecutionResult result = processQuery(CAN_DELETE_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map expectedAccessForGroupOne = singletonMap(GdAdGroup.ACCESS.name(),
                singletonMap(CAN_DELETED_AD_GROUP_RESOLVER_NAME, true));
        Map expectedAccessForGroupThree = singletonMap(GdAdGroup.ACCESS.name(),
                singletonMap(CAN_DELETED_AD_GROUP_RESOLVER_NAME, false));
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.FEATURES.name(),
                                singletonMap(CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME, 1))
                        .put(GdAdGroupsContext.ROWSET.name(),
                                Arrays.asList(expectedAccessForGroupOne, expectedAccessForGroupThree))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCanDeleteAdGroupsResolvers_whenAdGroupsIsEmpty() {
        List<AdGroupInfo> emptyAdGroupsInfo = Arrays.asList(groupInfoOne, groupInfoThree);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(listToSet(emptyAdGroupsInfo, AdGroupInfo::getCampaignId))
                .withAdGroupIdIn(listToSet(emptyAdGroupsInfo, AdGroupInfo::getAdGroupId));
        ExecutionResult result = processQuery(CAN_DELETE_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map expectedAccess = singletonMap(GdAdGroup.ACCESS.name(),
                singletonMap(CAN_DELETED_AD_GROUP_RESOLVER_NAME, true));
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.FEATURES.name(),
                                singletonMap(CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME, emptyAdGroupsInfo.size()))
                        .put(GdAdGroupsContext.ROWSET.name(), mapList(emptyAdGroupsInfo, ignore -> expectedAccess))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCanSentToBSAndToModerationAdGroupsResolvers() {
        var clientInfo = userInfo.getClientInfo();
        //Для сапортов доступна операция перемодерации и принятия на модерации коллаутов
        userInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).getChiefUserInfo();
        context = ContextHelper.buildContext(userInfo.getUser(), clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        AdGroupInfo adGroupWithAdAndShowConditions = groupInfoThree;
        steps.keywordSteps().createKeyword(adGroupWithAdAndShowConditions);
        bannerSteps.createActiveTextBanner(adGroupWithAdAndShowConditions);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(
                        ImmutableSet.of(groupInfoOne.getCampaignId(), adGroupWithAdAndShowConditions.getCampaignId()))
                .withAdGroupIdIn(
                        ImmutableSet.of(groupInfoOne.getAdGroupId(), adGroupWithAdAndShowConditions.getAdGroupId()));
        ExecutionResult result = processQuery(CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map expectedAccessForGroupOne = singletonMap(GdAdGroup.ACCESS.name(), ImmutableMap.builder()
                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                .build());
        Map expectedAccessForGroupWithAdAndShowConditions = singletonMap(GdAdGroup.ACCESS.name(), ImmutableMap.builder()
                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, true)
                .build());
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.FEATURES.name(), ImmutableMap.builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 1)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 1)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 1)
                                .build())
                        .put(GdAdGroupsContext.ROWSET.name(),
                                asList(expectedAccessForGroupOne, expectedAccessForGroupWithAdAndShowConditions))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCanAcceptAndRemoderateAdsCalloutsResolvers() {
        var clientInfo = userInfo.getClientInfo();
        //Для сапортов доступна операция перемодерации и принятия на модерации коллаутов
        userInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).getChiefUserInfo();
        context = ContextHelper.buildContext(userInfo.getUser(), clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        AdGroupInfo adGroupWithCalloutsInfo = groupInfoThree;
        Callout callout = steps.calloutSteps().createDefaultCallout(clientInfo);
        OldTextBanner textBanner =
                defaultTextBanner(adGroupWithCalloutsInfo.getCampaignId(), adGroupWithCalloutsInfo.getAdGroupId());
        textBanner.setCalloutIds(singletonList(callout.getId()));
        bannerSteps.createBanner(textBanner, adGroupWithCalloutsInfo);
        adGroupsContainer.getFilter()
                .withCampaignIdIn(
                        ImmutableSet.of(groupInfoOne.getCampaignId(), adGroupWithCalloutsInfo.getCampaignId()))
                .withAdGroupIdIn(ImmutableSet.of(groupInfoOne.getAdGroupId(), adGroupWithCalloutsInfo.getAdGroupId()));
        ExecutionResult result = processQuery(CAN_ACCEPT_AND_REMODERATE_ADS_CALLOUTS_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map expectedAccessForGroupOne = singletonMap(GdAdGroup.ACCESS.name(), ImmutableMap.builder()
                .put(CAN_REMODERATE_ADS_CALLOUTS_RESOLVER_NAME, false)
                .put(CAN_ACCEPT_ADS_CALLOUTS_MODERATION_RESOLVER_NAME, false)
                .build());
        Map expectedAccessForGroupWithCallouts = singletonMap(GdAdGroup.ACCESS.name(), ImmutableMap.builder()
                .put(CAN_REMODERATE_ADS_CALLOUTS_RESOLVER_NAME, true)
                .put(CAN_ACCEPT_ADS_CALLOUTS_MODERATION_RESOLVER_NAME, true)
                .build());
        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.FEATURES.name(), ImmutableMap.builder()
                                .put(CAN_REMODERATE_ADS_CALLOUTS_COUNT_RESOLVER_NAME, 1)
                                .put(CAN_ACCEPT_ADS_CALLOUTS_MODERATION_COUNT_RESOLVER_NAME, 1)
                                .build())
                        .put(GdAdGroupsContext.ROWSET.name(),
                                Arrays.asList(expectedAccessForGroupOne, expectedAccessForGroupWithCallouts))
                        .build()
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testLibraryMwIdFilter_TwoAdGroupsMatches() {
        Long libMwId1 = groupInfoOne.getAdGroup().getLibraryMinusKeywordsIds().get(0);
        Long libMwId2 = groupInfoThree.getAdGroup().getLibraryMinusKeywordsIds().get(0);
        adGroupsContainer.getFilter().withLibraryMwIdIn(new HashSet<>(asList(libMwId1, libMwId2)));

        ExecutionResult result = processQuery(QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());
        Map<String, Object> data = result.getData();

        List<Map> expectedRowset = getExpectedRowset(groupInfoOne, groupInfoThree);
        Map<String, Object> expected = singletonMap(
                "client",
                ImmutableMap.of("adGroups", ImmutableMap.of(
                        "totalCount", 2,
                        "rowset", expectedRowset)
                )
        );
        BeanFieldPath prefix = newPath("client", "adGroups");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("filter"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("adGroupIds"))
                .useMatcher(containsInAnyOrder(groupInfoOne.getAdGroupId(), groupInfoThree.getAdGroupId()));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void testLibraryMwIdFilter_NothingMatches() {
        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(groupInfoOne.getClientInfo());
        adGroupsContainer.getFilter().withLibraryMwIdIn(singleton(libraryMinusKeywordsPack.getMinusKeywordPackId()));

        ExecutionResult result = processQuery(QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());
        Map<String, Object> data = result.getData();

        List<Map> expectedRowset = emptyList();
        Map<String, Object> expected = singletonMap(
                "client",
                ImmutableMap.of("adGroups", ImmutableMap.of(
                        "totalCount", 0,
                        "rowset", expectedRowset)
                )
        );
        BeanFieldPath prefix = newPath("client", "adGroups");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("filter"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("adGroupIds"))
                .useMatcher(hasSize(0));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void testMwPackNameFilter_NothingMatches() {
        ClientInfo clientInfo = groupInfoOne.getClientInfo();
        Long packId1 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("first pack"), clientInfo).getMinusKeywordPackId();
        Long packId2 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("second pack"), clientInfo).getMinusKeywordPackId();
        Long adGroupId1 = groupInfoOne.getAdGroupId();
        Long adGroupId2 = groupInfoThree.getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), packId1,
                adGroupId1);
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), packId2,
                adGroupId2);

        adGroupsContainer.getFilter()
                .withAdGroupIdIn(ImmutableSet.of(adGroupId1, adGroupId2))
                .withMwPackNameContains("third");

        ExecutionResult result = processQuery(QUERY_TEMPLATE);
        GraphQLUtils.logErrors(result.getErrors());
        Map<String, Object> data = result.getData();

        List<Map> expectedRowset = emptyList();
        Map<String, Object> expected = singletonMap(
                "client",
                ImmutableMap.of("adGroups", ImmutableMap.of(
                        "totalCount", 0,
                        "rowset", expectedRowset)
                )
        );
        BeanFieldPath prefix = newPath("client", "adGroups");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("filter"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("adGroupIds"))
                .useMatcher(hasSize(0));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    private ExecutionResult processQuery(String queryTemplate) {
        String query = String.format(queryTemplate, context.getSubjectUser().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private ExecutionResult processQuery(String queryTemplate, Object parameter) {
        String query = String.format(queryTemplate, graphQlSerialize(parameter));
        return processor.processQuery(null, query, null, context);
    }

    private List<Map> getExpectedRowset(AdGroupInfo... adGroups) {
        Counter counter = new Counter();
        return StreamEx.of(adGroups)
                .map(AdGroupInfo::getAdGroup)
                .map(adGroup -> {
                    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                    builder
                            .put(GdAdGroup.INDEX.name(), counter.next())
                            .put(GdAdGroup.ID.name(), adGroup.getId())
                            .put(GdAdGroup.MINUS_KEYWORDS.name(), adGroup.getMinusKeywords())
                            .put(GdAdGroup.LIBRARY_MINUS_KEYWORDS_PACKS.name(),
                                    mapList(adGroup.getLibraryMinusKeywordsIds(), id -> {
                                        MinusKeywordsPack minusKeywordsPack = existingMinusKeywordsPacks.get(id);
                                        return toMinusKeywordPackInfoMap(minusKeywordsPack);
                                    }))
                            .put(GdAdGroup.REGIONS_INFO.name(), singletonMap(
                                    GdiAdGroupRegionsInfo.REGION_IDS.name(),
                                    ListUtils.longToIntegerList(adGroup.getGeo())))
                            .put(GdAdGroup.CAMPAIGN.name(),
                                    Map.of(GdCampaignTruncated.ID.name(), adGroup.getCampaignId(),
                                           GdCampaignTruncated.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name(), false));
                    return builder.build();
                })
                .map(Map.class::cast)
                .toList();
    }

    private Map<String, Object> toMinusKeywordPackInfoMap(MinusKeywordsPack minusKeywordsPack) {
        return ImmutableMap.of(
                GdMinusKeywordsPackInfo.ID.name(), minusKeywordsPack.getId(),
                GdMinusKeywordsPackInfo.NAME.name(), minusKeywordsPack.getName());
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
}
