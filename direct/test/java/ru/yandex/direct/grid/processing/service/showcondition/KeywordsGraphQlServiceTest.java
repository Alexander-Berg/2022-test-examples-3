package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.RelevanceMatchSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdDeleteKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSuspendKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdUnsuspendKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdUpdateKeywordsItem;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdUpdateKeywordsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.KeywordsTestUtils.toUpdateKeywordsItemData;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordsGraphQlServiceTest {

    private static final String DELETE_KEYWORDS_MUTATION = "deleteKeywords";
    private static final String SUSPEND_KEYWORDS_MUTATION = "suspendKeywords";
    private static final String UNSUSPEND_KEYWORDS_MUTATION = "unsuspendKeywords";
    private static final String UPDATE_KEYWORDS_MUTATION = "updateKeywords";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: {keywordIds: [%s]}) {\n"
            + "    %s\n"
            + "  }\n"
            + "}";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  updateKeywords(input: {keywordUpdateItems: %s}) {\n"
            + "    updatedKeywords {\n"
            + "      ... updateItem"
            + "    },"
            + "    affectedKeywords {\n"
            + "      ... updateItem"
            + "    }"
            + "  }\n"
            + "}"
            + "fragment updateItem on GdUpdateKeywordsPayloadItem {\n"
            + "  id,\n"
            + "  keyword\n"
            + "  minusKeywords,\n"
            + "  duplicate,\n"
            + "  isSuspended\n"
            + "}";

    private User operator;
    private AdGroupInfo adGroupInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private KeywordSteps keywordSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RelevanceMatchSteps relevanceMatchSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RelevanceMatchService relevanceMatchService;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createDefaultUser();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        contextProvider.setGridContext(buildContext(operator));

        adGroupInfo = new AdGroupInfo()
                .withClientInfo(userInfo.getClientInfo());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void deleteKeywords() {
        KeywordInfo keyword = keywordSteps.createKeyword(adGroupInfo);
        Long relevanceMatchId = relevanceMatchSteps.addDefaultRelevanceMatchToAdGroup(adGroupInfo);

        String query = String.format(MUTATION_TEMPLATE, DELETE_KEYWORDS_MUTATION,
                keyword.getId() + "," + relevanceMatchId,
                GdDeleteKeywordsPayload.DELETED_KEYWORD_IDS.name());

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(DELETE_KEYWORDS_MUTATION,
                getExpectedData(GdDeleteKeywordsPayload.DELETED_KEYWORD_IDS.name(), keyword.getId(), relevanceMatchId));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));

        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), singletonList(keyword.getId()));
        List<RelevanceMatch> relevanceMatchIds = relevanceMatchService.getRelevanceMatchByIds(operator.getClientId(),
                singletonList(relevanceMatchId));
        assertThat(keywords).isEmpty();
        assertThat(relevanceMatchIds).isEmpty();
    }

    @Test
    public void suspendKeywords() {
        KeywordInfo keyword = keywordSteps.createKeyword(adGroupInfo, defaultKeyword().withIsSuspended(false));
        Long relevanceMatchId = relevanceMatchSteps.addDefaultRelevanceMatchToAdGroup(adGroupInfo);
        String query = String.format(MUTATION_TEMPLATE, SUSPEND_KEYWORDS_MUTATION,
                keyword.getId() + "," + relevanceMatchId,
                GdSuspendKeywordsPayload.SUSPENDED_KEYWORD_IDS.name());

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(SUSPEND_KEYWORDS_MUTATION,
                getExpectedData(GdSuspendKeywordsPayload.SUSPENDED_KEYWORD_IDS.name(), keyword.getId(),
                        relevanceMatchId));

        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), singletonList(keyword.getId()));
        List<RelevanceMatch> relevanceMatches =
                relevanceMatchService.getRelevanceMatchByIds(operator.getClientId(),
                        singletonList(relevanceMatchId));

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
        softAssertions.assertThat(keywords)
                .size().isEqualTo(1);
        softAssertions.assertThat(keywords.get(0).getIsSuspended())
                .isTrue();
        softAssertions.assertThat(relevanceMatches)
                .size().isEqualTo(1);
        softAssertions.assertThat(relevanceMatches.get(0).getIsSuspended())
                .isTrue();
        softAssertions.assertAll();
    }

    @Test
    public void unsuspendKeywords() {
        KeywordInfo keyword = keywordSteps.createKeyword(adGroupInfo, defaultKeyword().withIsSuspended(true));
        Long relevanceMatchId =
                relevanceMatchSteps.addRelevanceMatchToAdGroup(singletonList(relevanceMatchSteps.getDefaultRelevanceMatch(adGroupInfo).withIsSuspended(true)), adGroupInfo).get(0);

        String query = String.format(MUTATION_TEMPLATE, UNSUSPEND_KEYWORDS_MUTATION,
                keyword.getId() + "," + relevanceMatchId,
                GdUnsuspendKeywordsPayload.UNSUSPENDED_KEYWORD_IDS.name());

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(UNSUSPEND_KEYWORDS_MUTATION,
                getExpectedData(GdUnsuspendKeywordsPayload.UNSUSPENDED_KEYWORD_IDS.name(), keyword.getId(),
                        relevanceMatchId));
        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), singletonList(keyword.getId()));
        List<RelevanceMatch> relevanceMatches =
                relevanceMatchService.getRelevanceMatchByIds(operator.getClientId(),
                        singletonList(relevanceMatchId));

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(data)
                .is(matchedBy(beanDiffer(expected)));

        softAssertions.assertThat(keywords)
                .size().isEqualTo(1);
        softAssertions.assertThat(keywords.get(0).getIsSuspended())
                .isFalse();
        softAssertions.assertThat(relevanceMatches)
                .size().isEqualTo(1);
        softAssertions.assertThat(relevanceMatches.get(0).getIsSuspended())
                .isFalse();
        softAssertions.assertAll();
    }

    @Test
    public void unsuspendKeywords_whenCampaignIsArchived_failure() {
        Keyword keyword = defaultKeyword().withIsSuspended(true);
        Long keywordId = keywordSteps.createKeyword(adGroupInfo, keyword).getId();
        campaignSteps.archiveCampaign(adGroupInfo.getShard(), adGroupInfo.getCampaignId());

        String query = String.format(MUTATION_TEMPLATE, UNSUSPEND_KEYWORDS_MUTATION, keywordId,
                GdUnsuspendKeywordsPayload.UNSUSPENDED_KEYWORD_IDS.name());
        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        List<Long> unsuspendedKeywordIds = getDataValue(data, "unsuspendKeywords/unsuspendedKeywordIds");
        Keyword actualKeyword = keywordService.getKeywords(operator.getClientId(), singletonList(keywordId)).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(unsuspendedKeywordIds).as("unsuspendedKeywordIds").isEmpty();
            soft.assertThat(actualKeyword.getIsSuspended()).as("actualKeyword#IsSuspended").isTrue();
        });
    }

    @Test
    public void updateKeywords() {
        KeywordInfo keyword = keywordSteps.createKeyword(adGroupInfo, keywordWithText("конь -слон")
                .withPrice(BigDecimal.ONE)
                .withPriceContext(BigDecimal.TEN)
        );
        KeywordInfo keyword2 = keywordSteps.createKeyword(adGroupInfo, keywordWithText("кота"));
        Keyword editedKeyword = toEditedKeyword(keyword);
        GdUpdateKeywordsItem updateItem = toGdUpdateKeywordsItem(editedKeyword);
        String query = String.format(UPDATE_MUTATION_TEMPLATE, graphQlSerialize(Collections.singleton(updateItem)));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Keyword expectedAffectedKeyword = new Keyword()
                .withId(keyword2.getId())
                .withIsSuspended(false)
                .withPhrase(keyword2.getKeyword().getPhrase() + " -купить");
        GdUpdateKeywordsPayload expectedPayload = new GdUpdateKeywordsPayload()
                .withUpdatedKeywords(singletonList(toUpdateKeywordsItemData(editedKeyword)))
                .withAffectedKeywords(singletonList(toUpdateKeywordsItemData(expectedAffectedKeyword)));
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(UPDATE_KEYWORDS_MUTATION);

        GdUpdateKeywordsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(UPDATE_KEYWORDS_MUTATION), GdUpdateKeywordsPayload.class);
        payload.getUpdatedKeywords().sort(Comparator.comparing(ModelWithId::getId));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), Arrays.asList(keyword.getId(), keyword2.getId()));
        keywords.sort(Comparator.comparing(Keyword::getId));

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());
        assertThat(keywords)
                .is(matchedBy(beanDiffer(Arrays.asList(editedKeyword, expectedAffectedKeyword))
                        .useCompareStrategy(compareStrategy))
                );
    }

    @Test
    public void updateKeywords_success_onEmptyInputList() {
        String query = String.format(UPDATE_MUTATION_TEMPLATE, graphQlSerialize(Collections.emptyList()));
        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .as("errors")
                .isEmpty();
    }

    @Test
    public void deleteKeywords_success_onEmptyInputList() {
        String query = String.format(MUTATION_TEMPLATE,
                DELETE_KEYWORDS_MUTATION /* mutation name */,
                "" /* empty id list */,
                GdDeleteKeywordsPayload.DELETED_KEYWORD_IDS.name() /* payload name */
        );
        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .as("errors")
                .isEmpty();
    }

    //helper methods
    private static Keyword toEditedKeyword(KeywordInfo keyword) {
        return new Keyword()
                .withId(keyword.getId())
                .withPhrase("Кот Купил")
                .withPrice(keyword.getKeyword().getPrice().add(BigDecimal.ONE))
                .withPriceContext(keyword.getKeyword().getPriceContext().add(BigDecimal.ONE))
                .withIsSuspended(false)
                .withAutobudgetPriority(3);
    }

    private static GdUpdateKeywordsItem toGdUpdateKeywordsItem(Keyword editedKeyword) {
        return new GdUpdateKeywordsItem()
                .withId(editedKeyword.getId())
                .withKeyword(editedKeyword.getPhrase())
                .withPrice(editedKeyword.getPrice())
                .withPriceContext(editedKeyword.getPriceContext())
                .withAutobudgetPriority(editedKeyword.getAutobudgetPriority());
    }

    private static Map<String, Object> getExpectedData(String fieldName, Long... keywordIds) {
        return ImmutableMap.of(fieldName, Arrays.asList(keywordIds));
    }

}

