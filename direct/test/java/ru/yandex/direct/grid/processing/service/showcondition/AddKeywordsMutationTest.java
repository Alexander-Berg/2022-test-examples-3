package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywordsItem;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywordsPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.showcondition.keywords.KeywordsDataService;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.UpdateDataHelper.toUidAndClientId;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class AddKeywordsMutationTest {

    private static final String KEYWORD = "yandex the best";
    private static final String AUTOTARGETING_PREFIX = "---autotargeting ";
    private static final long NOT_EXIST_AD_GROUP_ID = Long.MAX_VALUE;
    private static final String ADD_KEYWORDS_MUTATION = "addKeywords";
    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedItems {\n"
            + "      adGroupId,\n"
            + "      keywordId\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private User operator;
    private long adGroupId;
    private GdAddKeywords request;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private KeywordsDataService keywordsDataService;

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordService keywordService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.AUTOTARGETING_KEYWORD_PREFIX_ALLOWED, true);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);

        request = new GdAddKeywords()
                .withAddItems(Collections.singletonList(new GdAddKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withKeyword(KEYWORD))
                );
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void checkAddKeywords() {
        String query = String.format(ADD_MUTATION_TEMPLATE, ADD_KEYWORDS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Keyword expectedKeyword = new Keyword()
                .withIsSuspended(false)
                .withPhrase(KEYWORD)
                .withIsAutotargeting(false);
        Map<Long, List<Keyword>> keywordsByAdGroupIds =
                keywordService.getKeywordsByAdGroupIds(operator.getClientId(), Collections.singletonList(adGroupId));

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Keyword.ID.name())).useMatcher(notNullValue());
        assertThat(keywordsByAdGroupIds.get(adGroupId))
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedKeyword))
                        .useCompareStrategy(compareStrategy))
                );

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(ADD_KEYWORDS_MUTATION);

        GdAddKeywordsPayload expectedPayload = new GdAddKeywordsPayload()
                .withAddedItems(Collections.singletonList(new GdAddKeywordsPayloadItem()
                        .withAdGroupId(adGroupId)
                        .withKeywordId(keywordsByAdGroupIds.get(adGroupId).get(0).getId()))
                );
        GdAddKeywordsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(ADD_KEYWORDS_MUTATION), GdAddKeywordsPayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkAddKeywords_withAutotargetingPrefix() {
        String keyword = AUTOTARGETING_PREFIX + KEYWORD;
        GdAddKeywords gdAddKeywords = new GdAddKeywords()
                .withAddItems(Collections.singletonList(new GdAddKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withKeyword(keyword))
                );
        String query = String.format(ADD_MUTATION_TEMPLATE, ADD_KEYWORDS_MUTATION, graphQlSerialize(gdAddKeywords));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Keyword expectedKeyword = new Keyword()
                .withIsSuspended(false)
                .withPhrase(KEYWORD)
                .withIsAutotargeting(true);
        Map<Long, List<Keyword>> keywordsByAdGroupIds =
                keywordService.getKeywordsByAdGroupIds(operator.getClientId(), Collections.singletonList(adGroupId));

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Keyword.ID.name())).useMatcher(notNullValue());
        assertThat(keywordsByAdGroupIds.get(adGroupId))
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedKeyword))
                        .useCompareStrategy(compareStrategy))
                );

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(ADD_KEYWORDS_MUTATION);

        GdAddKeywordsPayload expectedPayload = new GdAddKeywordsPayload()
                .withAddedItems(Collections.singletonList(new GdAddKeywordsPayloadItem()
                        .withAdGroupId(adGroupId)
                        .withKeywordId(keywordsByAdGroupIds.get(adGroupId).get(0).getId()))
                );
        GdAddKeywordsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(ADD_KEYWORDS_MUTATION), GdAddKeywordsPayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkAddKeywords_RequestValidation() {
        request.setAddItems(Collections.singletonList(new GdAddKeywordsItem()
                .withAdGroupId(-1L) // невалидный ID
                .withKeyword(null) // любопытно, что сейчас валидация это пропускает
        ));
        Path pathWithError = path(field(GdAddKeywords.ADD_ITEMS), index(0), field(GdAddKeywordsItem.AD_GROUP_ID));
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(pathWithError, CommonDefects.validId()))));

        keywordsDataService.addKeywords(request, operator.getUid(), toUidAndClientId(operator));
    }

    @Test
    public void addKeywords_success_onEmptyItemList() {
        request.setAddItems(Collections.emptyList());
        GdAddKeywordsPayload payload =
                keywordsDataService.addKeywords(request, operator.getUid(), toUidAndClientId(operator));

        GdAddKeywordsPayload expectedPayload = new GdAddKeywordsPayload()
                .withAddedItems(Collections.emptyList())
                .withValidationResult(null);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkAddKeywords_CoreValidation() {
        request.getAddItems().get(0).setAdGroupId(NOT_EXIST_AD_GROUP_ID);

        GdAddKeywordsPayload payload =
                keywordsDataService.addKeywords(request, operator.getUid(), toUidAndClientId(operator));

        Path expectedPath = path(field(GdAddKeywords.ADD_ITEMS), index(0));
        GdAddKeywordsPayload expectedPayload = new GdAddKeywordsPayload()
                .withAddedItems(Collections.emptyList())
                .withValidationResult(toGdValidationResult(expectedPath, KeywordDefects.adGroupNotFound()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkAddKeywords_SuccessResult_WithValidationResult() {
        request.setAddItems(Arrays.asList(
                new GdAddKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withKeyword(KEYWORD),
                new GdAddKeywordsItem()
                        .withAdGroupId(NOT_EXIST_AD_GROUP_ID)
                        .withKeyword(KEYWORD + " -someMinus"))
        );

        GdAddKeywordsPayload payload =
                keywordsDataService.addKeywords(request, operator.getUid(), toUidAndClientId(operator));

        Map<Long, List<Keyword>> keywordsByAdGroupIds =
                keywordService.getKeywordsByAdGroupIds(operator.getClientId(), Collections.singletonList(adGroupId));

        Path expectedPath = path(field(GdAddKeywords.ADD_ITEMS), index(1));
        GdAddKeywordsPayload expectedPayload = new GdAddKeywordsPayload()
                .withAddedItems(Collections.singletonList(new GdAddKeywordsPayloadItem()
                        .withAdGroupId(adGroupId)
                        .withKeywordId(keywordsByAdGroupIds.get(adGroupId).get(0).getId())))
                .withValidationResult(toGdValidationResult(expectedPath, KeywordDefects.adGroupNotFound()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

}
