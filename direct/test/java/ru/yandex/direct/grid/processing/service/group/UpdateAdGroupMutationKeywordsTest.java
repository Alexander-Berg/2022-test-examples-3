package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;

import static ru.yandex.direct.grid.processing.service.group.AdGroupMutationService.PATH_FOR_UPDATE_TEXT_AD_GROUP;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupMutationKeywordsTest extends UpdateAdGroupMutationBaseTest {

    private static final String NEW_PHRASE = "купить бетон";
    private static final String NEW_NORM_PHRASE = "бетон купить";
    private static final String TOO_LONG_PHRASE = "dgfdfgdgfdfdgfdfgdfgdfgdfgdfdgfdfgfddgfdfgdgfdgfdfgdgfdffdgfdgdgfdgf";
    private static final BigDecimal GENERAL_PRICE = BigDecimal.valueOf(3.14);

    @Before
    public void init() {
        super.initTestData();
    }

    @Test
    public void checkUpdateAdGroupKeywords_addKeyword() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .addKeyword(null, NEW_PHRASE)
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        //проверяем изменения в самой группе
        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW));

        //изначальные ключевые фразы
        List<Keyword> expectedKeywords = mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword);

        checkKeywordDbState(expectedKeywords);

        //новая ключевая фраза
        Keyword expectedNewKeyword = new Keyword()
                .withAdGroupId(textAdGroupInfo.getAdGroupId())
                .withCampaignId(textAdGroupInfo.getCampaignId())
                .withPhrase(NEW_PHRASE)
                .withWordsCount(2);

        checkNewKeywordDbState(expectedNewKeyword, expectedKeywords, adGroupId);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupKeywords_removeKeyword() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .removeLastKeywordIfExists()
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        //проверяем изменения в самой группе
        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusBsSynced(StatusBsSynced.NO));

        //ключевые фразы
        List<Keyword> expectedKeywords = Collections.singletonList(keywordInfo[0].getKeyword());

        checkKeywordDbState(expectedKeywords);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupKeywords_removeAllKeywords() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setKeywords(Collections.emptyList())
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        //проверяем изменения в самой группе
        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusBsSynced(StatusBsSynced.NO));

        Map<Long, List<Keyword>> groupKeywords = keywordService
                .getKeywordsByAdGroupIds(clientInfo.getClientId(), Collections.singletonList(adGroupId));

        softAssertions.assertThat(groupKeywords)
                .isEmpty();

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupKeywords_updatePhrase() {
        keywordInfo[1].getKeyword()
                .withPhrase(NEW_PHRASE)
                .withWordsCount(2)
                .withNormPhrase(NEW_NORM_PHRASE);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setKeywords(createKeywordInputItem(keywordInfo[0]), createKeywordInputItem(keywordInfo[1]))
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusBsSynced(StatusBsSynced.NO));

        List<Keyword> expectedKeywords = Collections.singletonList(keywordInfo[0].getKeyword());

        checkKeywordDbState(expectedKeywords);

        softAssertions.assertAll();
    }


    @Test
    public void checkUpdateAdGroupKeywords_updateTooLongPhrase_andCheckValidationResult() {
        keywordInfo[1].getKeyword()
                .withPhrase(TOO_LONG_PHRASE)
                .withWordsCount(1)
                .withNormPhrase(TOO_LONG_PHRASE);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setKeywords(createKeywordInputItem(keywordInfo[0]), createKeywordInputItem(keywordInfo[1]))
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(PATH_FOR_UPDATE_TEXT_AD_GROUP), index(0),
                        field(GdUpdateTextAdGroupItem.KEYWORDS.name()), index(1), field(Keyword.PHRASE.name())),
                PhraseDefects.tooLongWord(PhraseConstraints.WORD_MAX_LENGTH, List.of(TOO_LONG_PHRASE)), true);
        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.emptyList())
                .withValidationResult(expectedGdValidationResult);

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupKeywords_changeGeneralPriceOnly() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeneralPrice(GENERAL_PRICE)
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkAdGroupsDbState(textAdGroupInfo.getAdGroup());

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupKeywords_addKeyword_generalPrice() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .addKeyword(null, NEW_PHRASE)
                .setGeneralPrice(GENERAL_PRICE)
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        //проверяем изменения в самой группе
        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW));

        //изначальные ключевые фразы
        List<Keyword> expectedKeywords = mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword);

        checkKeywordDbState(expectedKeywords);

        //новая ключевая фраза
        Keyword expectedNewKeyword = new Keyword()
                .withAdGroupId(textAdGroupInfo.getAdGroupId())
                .withCampaignId(textAdGroupInfo.getCampaignId())
                .withPhrase(NEW_PHRASE)
                .withWordsCount(2)
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE);

        checkNewKeywordDbState(expectedNewKeyword, expectedKeywords, adGroupId);

        softAssertions.assertAll();
    }

    @Test
    @Ignore("generalPrice пока не поддержан")
    public void checkUpdateAdGroupKeywords_updateKeyword_generalPrice() {
        keywordInfo[1].getKeyword()
                .withPhrase(NEW_PHRASE)
                .withWordsCount(2)
                .withNormPhrase(NEW_NORM_PHRASE);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setKeywords(createKeywordInputItem(keywordInfo[0]), createKeywordInputItem(keywordInfo[1]))
                .setGeneralPrice(GENERAL_PRICE)
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkAdGroupsDbState(textAdGroupInfo.getAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusBsSynced(StatusBsSynced.NO));

        List<Keyword> expectedKeywords = Arrays.asList(keywordInfo[0].getKeyword()
                        .withPrice(GENERAL_PRICE)
                        .withPriceContext(GENERAL_PRICE),
                keywordInfo[1].getKeyword());

        checkKeywordDbState(expectedKeywords);

        softAssertions.assertAll();
    }

}
