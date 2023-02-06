package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
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
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.findandreplace.ChangeMode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywordField;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywordsPreviewPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywordsPreviewPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.showcondition.converter.FindAndReplaceDataConverter;
import ru.yandex.direct.grid.processing.service.showcondition.keywords.FindAndReplaceDataService;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.KeywordsTestUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidPlusMark;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.FindAndReplaceDataConverter.getEmptyPayload;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.FindAndReplaceDataConverter.getEmptyPreviewPayload;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.searchOptions;
import static ru.yandex.direct.grid.processing.util.UpdateDataHelper.toUidAndClientId;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.filterAndMapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class FindAndReplaceKeywordsTest {

    private static final String NOT_FOUND_KEYWORDS_SEARCH_TEXT = "notFoundText";
    //невалидный т.к. после замены будет два подряд минуса в фразе
    private static final String INVALID_CHANGE_KEYWORD_FOR_MINUSES = "-жир";
    private static final String INVALID_PLUS_MARK_TEXT = "++invalid";
    private static final String FIND_AND_REPLACE_KEYWORDS_MUTATION_PREVIEW_QUERY = "getFindAndReplaceKeywordsPreview";
    private static final String FIND_AND_REPLACE_KEYWORDS_MUTATION = "findAndReplaceKeywords";
    private static final String PREVIEW_QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    cacheKey,\n"
            + "    totalCount,\n"
            + "    keywordIds,\n"
            + "    rowset {\n"
            + "      id,\n"
            + "      keyword\n"
            + "      minusKeywords,\n"
            + "      changedKeyword,\n"
            + "      changedMinusKeywords\n"
            + "    }\n"
            + "  }"
            + "}";
    private static final String FIND_AND_REPLACE_KEYWORDS_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    cacheKey,\n"
            + "    updatedTotalCount,\n"
            + "    updatedKeywordIds,\n"
            + "    affectedKeywords {\n"
            + "      ... updateItem\n"
            + "    }\n"
            + "    rowset {\n"
            + "      ... updateItem\n"
            + "    }\n"
            + "  }"
            + "}"
            + "fragment updateItem on GdUpdateKeywordsPayloadItem {\n"
            + "  id,\n"
            + "  keyword\n"
            + "  minusKeywords,\n"
            + "  duplicate,\n"
            + "  isSuspended\n"
            + "}";

    private GdFindAndReplaceKeywords request;
    private User operator;
    private Keyword keyword, keywordWithMinuses;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private FindAndReplaceDataService findAndReplaceDataService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);

        String searchText = "слон";
        KeywordInfo keywordInfo = steps.keywordSteps()
                .createKeyword(adGroupInfo, keywordWithText("Большой " + searchText));
        keyword = keywordInfo.getKeyword();
        KeywordInfo keywordInfo2 = steps.keywordSteps()
                .createKeyword(adGroupInfo, keywordWithText("конь -" + searchText));
        keywordWithMinuses = keywordInfo2.getKeyword();
        operator = UserHelper.getUser(keywordInfo);

        request = new GdFindAndReplaceKeywords()
                .withKeywordIds(Arrays.asList(keyword.getId(), keywordWithMinuses.getId()))
                .withChangeMode(ChangeMode.REPLACE)
                .withSearchText(searchText)
                .withChangeText("ЖираФ")
                .withFields(
                        EnumSet.of(GdFindAndReplaceKeywordField.KEYWORD, GdFindAndReplaceKeywordField.MINUS_KEYWORDS))
                .withSearchOptions(searchOptions(false, true));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void findAndReplaceKeywordsPreview() {
        String query = String.format(PREVIEW_QUERY_TEMPLATE, FIND_AND_REPLACE_KEYWORDS_MUTATION_PREVIEW_QUERY,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        GdFindAndReplaceKeywordsPreviewPayload expectedPayload = new GdFindAndReplaceKeywordsPreviewPayload()
                .withTotalCount(request.getKeywordIds().size())
                .withKeywordIds(request.getKeywordIds())
                .withRowset(getExpectedPreviewRowset());
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(FIND_AND_REPLACE_KEYWORDS_MUTATION_PREVIEW_QUERY);

        GdFindAndReplaceKeywordsPreviewPayload payload =
                GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_KEYWORDS_MUTATION_PREVIEW_QUERY),
                        GdFindAndReplaceKeywordsPreviewPayload.class);
        payload.getRowset().sort(Comparator.comparing(ModelWithId::getId));

        CompareStrategy strategy = DefaultCompareStrategies.allFields()
                .forFields(newPath(GdFindAndReplaceKeywordsPreviewPayload.CACHE_KEY.name()))
                .useMatcher(notNullValue());
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(strategy)));
    }

    @Test
    public void findAndReplaceKeyword() {
        TestAuthHelper.setDirectAuthentication(operator);

        String query = String.format(FIND_AND_REPLACE_KEYWORDS_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_KEYWORDS_MUTATION, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        List<Keyword> expectedKeywords = mapList(getExpectedPreviewRowset(), this::toKeyword);
        GdFindAndReplaceKeywordsPayload expectedPayload = new GdFindAndReplaceKeywordsPayload()
                .withUpdatedTotalCount(request.getKeywordIds().size())
                .withUpdatedKeywordIds(request.getKeywordIds())
                .withCacheKey("")
                .withAffectedKeywords(Collections.emptyList())
                .withRowset(mapList(expectedKeywords, KeywordsTestUtils::toUpdateKeywordsItemData));
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(FIND_AND_REPLACE_KEYWORDS_MUTATION);

        GdFindAndReplaceKeywordsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(FIND_AND_REPLACE_KEYWORDS_MUTATION), GdFindAndReplaceKeywordsPayload.class);
        payload.getRowset().sort(Comparator.comparing(ModelWithId::getId));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(),
                        Arrays.asList(keyword.getId(), keywordWithMinuses.getId()));
        keywords.sort(Comparator.comparing(Keyword::getId));
        assertThat(keywords)
                .is(matchedBy(beanDiffer(expectedKeywords)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()))
                );
    }

    @Test
    public void checkFindAndReplaceKeywordsPreview_RequestValidation() {
        request.withKeywordIds(Collections.emptyList());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(GdFindAndReplaceKeywords.KEYWORD_IDS.name(), CollectionDefects.notEmptyCollection()))));

        findAndReplaceDataService.getPreview(request, operator.getUid(), toUidAndClientId(operator));
    }

    @Test
    public void checkFindAndReplaceKeywordsPreview_ReturnEmptyPayload() {
        request.withSearchText(NOT_FOUND_KEYWORDS_SEARCH_TEXT);
        GdFindAndReplaceKeywordsPreviewPayload payload =
                findAndReplaceDataService.getPreview(request, operator.getUid(), toUidAndClientId(operator));

        GdFindAndReplaceKeywordsPreviewPayload expectedPayload = getEmptyPreviewPayload();
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceKeywordsPreview_CoreValidation() {
        request.withChangeText(INVALID_CHANGE_KEYWORD_FOR_MINUSES)
                .withKeywordIds(singletonList(keywordWithMinuses.getId()));
        GdFindAndReplaceKeywordsPreviewPayload payload =
                findAndReplaceDataService.getPreview(request, operator.getUid(), toUidAndClientId(operator));

        Path expectedPath = path(field(GdFindAndReplaceKeywords.KEYWORD_IDS), index(0));
        GdFindAndReplaceKeywordsPreviewPayload expectedPayload = new GdFindAndReplaceKeywordsPreviewPayload()
                .withTotalCount(request.getKeywordIds().size())
                .withKeywordIds(request.getKeywordIds())
                .withRowset(singletonList(toPreviewItem(keywordWithMinuses)
                        .withChangedMinusKeywords(singletonList(INVALID_CHANGE_KEYWORD_FOR_MINUSES))
                ))
                .withValidationResult(toGdValidationResult(expectedPath, PhraseDefects.invalidMinusMark()));

        CompareStrategy strategy = DefaultCompareStrategies.allFields()
                .forFields(newPath(GdFindAndReplaceKeywordsPreviewPayload.CACHE_KEY.name()))
                .useMatcher(notNullValue());
        payload.getRowset().sort(Comparator.comparing(ModelWithId::getId));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(strategy)));
    }

    @Test
    public void checkFindAndReplaceKeywordsPreview_SuccessResult_WithValidationResult() {
        request.withChangeText(INVALID_CHANGE_KEYWORD_FOR_MINUSES);
        GdFindAndReplaceKeywordsPreviewPayload payload = findAndReplaceDataService
                .getPreview(request, operator.getUid(), toUidAndClientId(operator));

        Path expectedPath = path(field(GdFindAndReplaceKeywords.KEYWORD_IDS), index(1));
        GdFindAndReplaceKeywordsPreviewPayload expectedPayload = new GdFindAndReplaceKeywordsPreviewPayload()
                .withTotalCount(request.getKeywordIds().size())
                .withKeywordIds(request.getKeywordIds())
                .withRowset(getExpectedPreviewRowset())
                .withValidationResult(toGdValidationResult(expectedPath, PhraseDefects.invalidMinusMark()))
                .withCacheKey("");

        CompareStrategy strategy = DefaultCompareStrategies.allFields()
                .forFields(newPath(GdFindAndReplaceKeywordsPreviewPayload.CACHE_KEY.name()))
                .useMatcher(notNullValue());
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(strategy)));
    }

    @Test
    public void checkFindAndReplaceKeywords_RequestValidation() {
        request.withChangeText(INVALID_PLUS_MARK_TEXT);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(GdFindAndReplaceKeywords.CHANGE_TEXT.name(), invalidPlusMark()))));

        findAndReplaceDataService.findAndReplaceKeywords(request, operator.getUid(), toUidAndClientId(operator));
    }

    @Test
    public void checkFindAndReplaceKeywords_ReturnEmptyPayload() {
        request.withSearchText(NOT_FOUND_KEYWORDS_SEARCH_TEXT);
        GdFindAndReplaceKeywordsPayload payload = findAndReplaceDataService
                .findAndReplaceKeywords(request, operator.getUid(), toUidAndClientId(operator));

        GdFindAndReplaceKeywordsPayload expectedPayload = getEmptyPayload();
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceKeywords_CoreValidation() {
        request.withChangeText(INVALID_CHANGE_KEYWORD_FOR_MINUSES)
                .withKeywordIds(singletonList(keywordWithMinuses.getId()));
        GdFindAndReplaceKeywordsPayload payload = findAndReplaceDataService
                .findAndReplaceKeywords(request, operator.getUid(), toUidAndClientId(operator));

        Path expectedPath = path(field(GdFindAndReplaceKeywords.KEYWORD_IDS), index(0));
        GdFindAndReplaceKeywordsPayload expectedPayload = getEmptyPayload()
                .withValidationResult(toGdValidationResult(expectedPath, PhraseDefects.invalidMinusMark()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceKeywords_SuccessResult_WithValidationResult() {
        request.withChangeText(INVALID_CHANGE_KEYWORD_FOR_MINUSES);
        GdFindAndReplaceKeywordsPayload payload = findAndReplaceDataService
                .findAndReplaceKeywords(request, operator.getUid(), toUidAndClientId(operator));

        List<Keyword> expectedKeywords =
                filterAndMapList(getExpectedPreviewRowset(), k -> k.getId().equals(keyword.getId()), this::toKeyword);
        Path expectedPath = path(field(GdFindAndReplaceKeywords.KEYWORD_IDS), index(1));
        GdFindAndReplaceKeywordsPayload expectedPayload = new GdFindAndReplaceKeywordsPayload()
                .withUpdatedTotalCount(expectedKeywords.size())
                .withUpdatedKeywordIds(singletonList(keyword.getId()))
                .withRowset(mapList(expectedKeywords, KeywordsTestUtils::toUpdateKeywordsItemData))
                .withValidationResult(toGdValidationResult(expectedPath, PhraseDefects.invalidMinusMark()))
                .withAffectedKeywords(Collections.emptyList())
                .withCacheKey("");

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceKeywords_CoreValidationIndexMapping() {
        request.withChangeText(INVALID_CHANGE_KEYWORD_FOR_MINUSES);
        GdFindAndReplaceKeywordsPayload payload = findAndReplaceDataService
                .findAndReplaceKeywords(request, operator.getUid(), toUidAndClientId(operator));

        Path expectedPath = path(field(GdFindAndReplaceKeywords.KEYWORD_IDS), index(1));
        GdValidationResult gdValidationResult = toGdValidationResult(expectedPath, PhraseDefects.invalidMinusMark());

        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(gdValidationResult)));
    }


    //helper methods
    private List<GdFindAndReplaceKeywordsPreviewPayloadItem> getExpectedPreviewRowset() {
        GdFindAndReplaceKeywordsPreviewPayloadItem expectedPreviewForKeyword = toPreviewItem(keyword);
        expectedPreviewForKeyword.withChangedKeyword(expectedPreviewForKeyword.getKeyword()
                .replace(request.getSearchText(), request.getChangeText()));

        GdFindAndReplaceKeywordsPreviewPayloadItem expectedPreviewForKeyword2 = toPreviewItem(keywordWithMinuses);
        assumeThat(expectedPreviewForKeyword2.getMinusKeywords(), hasSize(1));
        String changedMinusKeyword = expectedPreviewForKeyword2.getMinusKeywords().get(0)
                .replace(request.getSearchText(), request.getChangeText());
        expectedPreviewForKeyword2.withChangedMinusKeywords(singletonList(changedMinusKeyword));
        return Arrays.asList(expectedPreviewForKeyword, expectedPreviewForKeyword2);
    }

    private static GdFindAndReplaceKeywordsPreviewPayloadItem toPreviewItem(Keyword keyword) {
        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(keyword.getPhrase());
        List<String> minusKeywords = mapList(keywordWithMinuses.getMinusKeywords(),
                ru.yandex.direct.libs.keywordutils.model.Keyword::toString);

        return new GdFindAndReplaceKeywordsPreviewPayloadItem()
                .withId(keyword.getId())
                .withKeyword(keywordWithMinuses.getKeyword().toString())
                .withMinusKeywords(minusKeywords);
    }

    private Keyword toKeyword(GdFindAndReplaceKeywordsPreviewPayloadItem previewItem) {
        return new Keyword()
                .withId(previewItem.getId())
                .withPhrase(FindAndReplaceDataConverter.toPhrase(previewItem))
                .withIsSuspended(false);
    }
}
