package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.resultPhrase;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithMinus;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithMinusAndFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithMinuses;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationUngluingOfUpdatedPhrasesTest extends KeywordsUpdateOperationBaseTest {

    // расклейка не производится между разными группами

    @Test
    public void execute_TwoUpdatedPhrasesSuitableForUngluingButInDifferentAdGroups() {
        createTwoActiveAdGroups();
        Long firstAdGroupKeywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long secondAdGroupKeywordIdToUpdate = createKeyword(adGroupInfo2, PHRASE_2).getId();

        String phrase1 = "кот";
        String phrase2 = "кот купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(firstAdGroupKeywordIdToUpdate, phrase1),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(firstAdGroupKeywordIdToUpdate, phrase1),
                isUpdated(secondAdGroupKeywordIdToUpdate, phrase2)));
    }

    @Test
    public void execute_UnglueWontRunIfItIsNotEnabled() {
        createOneActiveAdGroup();
        Long firstAdGroupKeywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long secondAdGroupKeywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "кот";
        String phrase2 = "кот купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(firstAdGroupKeywordIdToUpdate, phrase1),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartialWithoutUnglue(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(firstAdGroupKeywordIdToUpdate, phrase1),
                isUpdated(secondAdGroupKeywordIdToUpdate, phrase2)));
    }

    @Test
    public void execute_OneUpdatedPhraseAndOneOtherExistingSuitableForUngluingButInDifferentAdGroups() {
        String existingPhrase = "кот купить";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo2, PHRASE_1).getId();

        String newPhrase = "кот";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByOneMinusAndOneIsNotAppendedByInDifferentAdGroup() {
        createTwoActiveAdGroups();
        Long firstAdGroupKeywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long firstAdGroupKeywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long secondAdGroupKeywordIdToUpdate = createKeyword(adGroupInfo2, PHRASE_1).getId();

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        String phrase3 = "слон";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(firstAdGroupKeywordIdToUpdate1, phrase1),
                keywordModelChanges(firstAdGroupKeywordIdToUpdate2, phrase2),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(firstAdGroupKeywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(firstAdGroupKeywordIdToUpdate2, phrase2, phrase2Minus),
                        isUpdated(secondAdGroupKeywordIdToUpdate, phrase3)));
    }

    // расклейка работает на нормальных формах с зафиксированными стоп-словами

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfUpdatedPhraseInNormalForm() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купил кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    // игнорирование регистра при расклейке

    @Test
    public void execute_OneUpdatedPhraseWithUpperCaseIsAppendedByMinusOfUpdatedPhraseInNormalForm() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купил кота";
        String phrase2 = "Кот";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfUpdatedPhraseWithUpperCaseInNormalForm() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "Купил Кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseWithUpperCaseIsAppendedByMinusOfExistingPhraseInNormalForm() {
        String existingPhrase = "купил кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "Кот";
        String newPhraseMinus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithMinus(keywordIdToUpdate, newPhrase, newPhraseMinus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfExistingPhraseWithUpperCaseInNormalForm() {
        String existingPhrase = "Купил Кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот";
        String newPhraseMinus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithMinus(keywordIdToUpdate, newPhrase, newPhraseMinus)));
    }

    // игнорирование концевых точек

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfUpdatedPhraseWithEndingPoint() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купил. кота.";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    // остальные кейсы

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfExistingPhraseInNormalForm() {
        String existingPhrase = "купил кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот";
        String newPhraseMinus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithMinus(keywordIdToUpdate, newPhrase, newPhraseMinus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByFixedMinus() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купить !слона";
        String phrase2 = "купить";
        String phrase2Minus = "!слона";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByFixedStopword() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "123 на";
        String phrase1Result = "123 +на";
        String phrase2 = "123";
        String phrase2Minus = "!на";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(keywordIdToUpdate1, phrase1Result, phrase1, phrase1Result),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsNotAppendedByUnfixedStopword() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "лететь на юг";
        String phrase2 = "лететь юг -север";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2)));
    }

    @Test
    public void execute_OneUpdatedPhraseWithMinusIsAppendedByOneMinus() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купить кота";
        String phrase2 = "кот -пушистый";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByTwoMinusesOfUpdatedPhrases() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        List<String> phrase2Minuses = asList("купить", "погладить");
        String phrase3 = "погладить кота";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinuses(keywordIdToUpdate2, phrase2, phrase2Minuses),
                        isUpdated(keywordIdToUpdate3, phrase3)));
    }

    @Test
    public void execute_OneUpdatedPhraseAndOneInvalidPhraseIsAppendedByOneMinus() {
        createOneActiveAdGroup();
        String phrase1 = "купить кота";
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, phrase1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();


        String phrase2 = "кот -пушистый";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, INVALID_PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        null,
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusOfUpdatedPhraseAndOfOtherExistingPhrase() {
        String existingPhrase = "коты не фотосинтезируют";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        List<String> phrase2Minuses = asList("купить", "фотосинтезировать");
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinuses(keywordIdToUpdate2, phrase2, phrase2Minuses)));
    }

    // phraseBeforeUnglue содержит исходную фразу после фиксации стоп-слов

    @Test
    public void execute_OneUpdatedPhraseWithFixedStopWordIsAppendedByMinus() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "123 на";
        String phrase1BeforeUnglue = "123 +на";
        String phrase1Minus = "кот";
        String phrase2 = "123 +на кот";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithMinusAndFixation(keywordIdToUpdate1, phrase1BeforeUnglue, phrase1Minus, phrase1,
                                phrase1BeforeUnglue),
                        isUpdated(keywordIdToUpdate2, phrase2)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusAndDifferFromNormalForm_PhraseBeforeUnglueIsNotNormal() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "не коту";
        String phrase1Minus = "чеширский";
        String phrase2 = "чеширский кот";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithMinus(keywordIdToUpdate1, phrase1, phrase1Minus),
                        isUpdated(keywordIdToUpdate2, phrase2)));
    }

    // дубликаты правильно учитываются при расклейке

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusesOfUpdatedDuplicatedPhrase() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "кота купить";
        String phrase2 = "кота";
        String phrase2Minus = "купить";
        String phrase3 = "купить кота";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus),
                        isNotUpdated(keywordIdToUpdate1, phrase3)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByMinusesOfPhraseDuplicatedWithOtherExistingPhrase() {
        String existingKeyword = "кота купить";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingKeyword).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "кота купить";
        String phrase2 = "кота";
        String phrase2Minus = "купить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotUpdated(existingKeywordId, phrase1),
                        isUpdatedWithMinus(keywordIdToUpdate2, phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsNotAppendedByMinusToAvoidDuplicationInUpdatedPhrases() {
        String existingKeyword = "кота купить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "кота -купить";
        String phrase2 = "кота";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsNotAppendedByMinusToAvoidDuplicationWithOtherExistingPhrases() {
        String existingKeyword = "кота -накормить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "кота накормил";
        String phrase2 = "кота";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsPartiallyAppendedByMinusToAvoidDuplicationInUpdatedPhrases() {
        String existingKeyword = "кота накормить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "кота -накормил -погладил";
        String phrase2 = "кота погладил";
        String phrase3 = "кота";
        String phrase3Minus = "погладить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2),
                        isUpdatedWithMinus(keywordIdToUpdate3, phrase3, phrase3Minus)));
    }

    @Test
    public void execute_OneUpdatedPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithOtherExistingPhrases() {
        String existingKeyword = "кота -накормил -погладил";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "кота накормить";
        String phrase2 = "кота погладить";
        String phrase3 = "кота";
        String phrase3Minus = "накормить";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2),
                        isUpdatedWithMinus(keywordIdToUpdate3, phrase3, phrase3Minus)));
    }

    // фразы, не подлежащие расклейке в одной группе

    @Test
    public void execute_TwoUpdatedPhrasesNotSuitableForUngluing() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "дикий кот";
        String phrase2 = "домашний кот";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, phrase1),
                isUpdated(keywordIdToUpdate2, phrase2)));
    }

    @Test
    public void execute_UpdatedAndOtherExistingPhraseNotSuitableForUngluing() {
        createOneActiveAdGroup();
        String existingPhrase = "дикий кот";
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "домашний пес";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
    }

    // смешанный кейс

    @Test
    public void execute_OneUpdatedPhraseIsAppendedByOneMinusAndSomeAreNotAppendedBy() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate4 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate5 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase1 = "купить кота";
        String phrase2 = "слон";
        String phrase3 = "кот";
        String phrase3Minus = "купить";
        String phrase4 = "пони";
        String phrase5 = "саламандра";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3),
                keywordModelChanges(keywordIdToUpdate4, phrase4),
                keywordModelChanges(keywordIdToUpdate5, phrase5));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2),
                        isUpdatedWithMinus(keywordIdToUpdate3, phrase3, phrase3Minus),
                        isUpdated(keywordIdToUpdate4, phrase4),
                        isUpdated(keywordIdToUpdate5, phrase5)));
    }

    // сохранение

    @Test
    public void execute_UngluedNewPhraseIsSavedCorrectly() {
        String existingPhrase = "кот пес";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот слоны";
        String newPhrase2 = "кот";
        List<String> newPhrase2Minuses = asList("слон", "пес");
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, newPhrase1),
                        isUpdatedWithMinuses(keywordIdToUpdate2, newPhrase2, newPhrase2Minuses)));

        Keyword expectedNewKeyword2 = new Keyword()
                .withId(keywordIdToUpdate2)
                .withPhrase(resultPhrase(newPhrase2, newPhrase2Minuses))
                .withNormPhrase(newPhrase2)
                .withWordsCount(1);

        Keyword actualNewKeyword2 = getKeyword(keywordIdToUpdate2);
        assertThat("новая фраза после расклейки соответствует ожидаемой",
                actualNewKeyword2,
                beanDiffer(expectedNewKeyword2).useCompareStrategy(onlyExpectedFields()));
    }

    // смешанный кейс

    @Test
    public void execute_ComplexUngluingWithNewAndExistingAndSuitableAndUnsuitableForUngluingPhrases() {
        String existingPhrase1 = "дикие коты";
        String existingPhrase2 = "деревянный пони";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        Long existingKeywordId2 = createKeyword(adGroupInfo1, existingPhrase2).getId();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, "first").getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, "second").getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, "third").getId();
        Long keywordIdToUpdate4 = createKeyword(adGroupInfo1, "fourth").getId();
        Long keywordIdToUpdate5 = createKeyword(adGroupInfo1, "fifth").getId();
        Long keywordIdToUpdate6 = createKeyword(adGroupInfo1, "sixth").getId();

        String phrase1 = "купить кота";
        String phrase2 = "слон";
        String phrase3 = "кот";
        List<String> phrase3Minuses = asList("купить", "дикий");
        String phrase4 = "деревянный пони купить";
        String phrase5 = "саламандра";
        String phrase6 = "пони";
        String phrase6Minus = "деревянный";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3),
                keywordModelChanges(keywordIdToUpdate4, phrase4),
                keywordModelChanges(keywordIdToUpdate5, phrase5),
                keywordModelChanges(keywordIdToUpdate6, phrase6));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase1),
                        isUpdated(keywordIdToUpdate2, phrase2),
                        isUpdatedWithMinuses(keywordIdToUpdate3, phrase3, phrase3Minuses),
                        isUpdated(keywordIdToUpdate4, phrase4),
                        isUpdated(keywordIdToUpdate5, phrase5),
                        isUpdatedWithMinus(keywordIdToUpdate6, phrase6, phrase6Minus)));

        Keyword existingKeyword2 = getKeyword(existingKeywordId2);
        assertThat(existingKeyword2.getPhrase(), equalTo(resultPhrase(existingPhrase2, "купить")));

        Keyword newKeyword3 = getKeyword(result.get(2).getResult().getId());
        assertThat(newKeyword3.getPhrase(), equalTo(resultPhrase(phrase3, phrase3Minuses)));

        Keyword newKeyword6 = getKeyword(result.get(5).getResult().getId());
        assertThat(newKeyword6.getPhrase(), equalTo(resultPhrase(phrase6, phrase6Minus)));
    }
}
