package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithMinus;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithMinusAndFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithMinuses;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.resultPhrase;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationUngluingOfNewPhrasesTest extends KeywordsAddOperationBaseTest {

    // расклейка не производится между разными группами

    @Test
    public void execute_TwoNewPhrasesSuitableForUngluingButInDifferentAdGroups() {
        createTwoActiveAdGroups();

        String phrase1 = "кот";
        String phrase2 = "кот купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo2, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isAdded(phrase2)));
    }

    @Test
    public void execute_UnglueWontRunIfItIsNotEnabled() {
        createOneActiveAdGroup();

        String phrase1 = "кот";
        String phrase2 = "кот купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartialWithOutUnglue(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseAndOneExistingSuitableForUngluingButInDifferentAdGroups() {
        String existingPhrase = "кот купить";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "кот";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo2, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByOneMinusAndOneIsNotAppendedByInDifferentAdGroup() {
        createTwoActiveAdGroups();

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        String phrase3 = "кот";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo2, phrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus),
                        isAdded(phrase3)));
    }

    // расклейка работает на нормальных формах с зафиксированными стоп-словами

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfNewPhraseInNormalForm() {
        createOneActiveAdGroup();

        String phrase1 = "купил кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    // игнорирование регистра при расклейке

    @Test
    public void execute_OneNewPhraseWithUpperCaseIsAppendedByMinusOfNewPhraseInNormalForm() {
        createOneActiveAdGroup();

        String phrase1 = "купил кота";
        String phrase2 = "Кот";
        String phrase2Minus = "купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfNewPhraseWithUpperCaseInNormalForm() {
        createOneActiveAdGroup();

        String phrase1 = "Купил Кота";
        String phrase2 = "кот";
        String phrase2Minus = "купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfExistingPhraseInNormalForm() {
        String existingPhrase = "купил кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "кот";
        String newPhraseMinus = "купить";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(newPhrase, newPhraseMinus)));
    }

    @Test
    public void execute_OneNewPhraseWithUpperCaseIsAppendedByMinusOfExistingPhraseInNormalForm() {
        String existingPhrase = "купил кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "Кот";
        String newPhraseMinus = "купить";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(newPhrase, newPhraseMinus)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfExistingPhraseWithUpperCaseInNormalForm() {
        String existingPhrase = "Купил Кота";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "кот";
        String newPhraseMinus = "купить";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(newPhrase, newPhraseMinus)));
    }

    // игнорирование концевых точек при расклейке

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfExistingPhraseWithEndingPoint() {
        String existingPhrase = "купил. кота.";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "кот";
        String newPhraseMinus = "купить";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(newPhrase, newPhraseMinus)));
    }

    // остальные кейсы

    @Test
    public void execute_OneNewPhraseIsAppendedByFixedMinus() {
        createOneActiveAdGroup();

        String phrase1 = "купить !слона";
        String phrase2 = "купить";
        String phrase2Minus = "!слона";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByFixedStopWord() {
        createOneActiveAdGroup();

        String phrase1 = "123 на";
        String phrase1Result = "123 +на";
        String phrase2 = "123";
        String phrase2Minus = "!на";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithFixation(phrase1Result, phrase1, phrase1Result),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsNotAppendedByUnfixedStopWord() {
        createOneActiveAdGroup();

        String phrase1 = "лететь на юг";
        String phrase2 = "лететь юг -север";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseWithMinusIsAppendedByOneMinus() {
        createOneActiveAdGroup();

        String phrase1 = "купить кота";
        String phrase2 = "кот -пушистый";
        String phrase2Minus = "купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByTwoMinusesOfNewPhrase() {
        createOneActiveAdGroup();

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        List<String> phrase2Minuses = asList("купить", "погладить");
        String phrase3 = "погладить кота";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinuses(phrase2, phrase2Minuses),
                        isAdded(phrase3)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByTwoMinusesOfExistingPhrase() {
        String existingPhrase1 = "коты не фотосинтезируют";
        String existingPhrase2 = "коты мягкие";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        createKeyword(adGroupInfo1, existingPhrase2);

        String phrase1 = "кот";
        List<String> phrase1Minuses = asList("фотосинтезировать", "мягкий");
        String phrase2 = "слон";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinuses(phrase1, phrase1Minuses),
                        isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfNewPhraseAndOfExistingPhrase() {
        String existingPhrase = "коты не фотосинтезируют";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String phrase1 = "купить кота";
        String phrase2 = "кот";
        List<String> phrase2Minuses = asList("купить", "фотосинтезировать");
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinuses(phrase2, phrase2Minuses)));
    }

    // phraseBeforeUnglue содержит исходную фразу после фиксации стоп-слов

    @Test
    public void execute_OneNewPhraseWithFixedStopWordIsAppendedByMinus_PhraseBeforeUnglueIsFixed() {
        createOneActiveAdGroup();

        String phrase1 = "123 на";
        String phrase1BeforeUnglue = "123 +на";
        String phrase1Minus = "кот";
        String phrase2 = "123 +на кот";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinusAndFixation(phrase1BeforeUnglue, phrase1Minus, phrase1, phrase1BeforeUnglue),
                        isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusAndDifferFromNormalForm_PhraseBeforeUnglueIsNotNormal() {
        createOneActiveAdGroup();

        String phrase1 = "не коту";
        String phrase1Minus = "чеширский";
        String phrase2 = "чеширский кот";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(phrase1, phrase1Minus),
                        isAdded(phrase2)));
    }

    // дубликаты правильно учитываются при расклейке

    @Test
    public void execute_OneNewPhraseIsAppendedByOneMinusOfNewDuplicatedPhrases() {
        createOneActiveAdGroup();

        String phrase1 = "кота купить";
        String phrase2 = "кота";
        String phrase2Minus = "купить";
        String phrase3 = "купить кота";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus),
                        isNotAdded(phrase3)));
    }

    @Test
    public void execute_OneNewPhraseIsAppendedByMinusOfPhraseDuplicatedWithExistingPhrase() {
        String existingKeyword = "кота купить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);

        String phrase1 = "кота купить";
        String phrase2 = "кота";
        String phrase2Minus = "купить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAdded(phrase1),
                        isAddedWithMinus(phrase2, phrase2Minus)));
    }

    // при расклейке не должно образовываться новых дубликатов

    @Test
    public void execute_OneNewPhraseIsNotAppendedByMinusToAvoidDuplicationInNewPhrases() {
        String existingKeyword = "кота купить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);

        String phrase1 = "кота -купить";
        String phrase2 = "кота";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseIsNotAppendedByMinusToAvoidDuplicationWithExistingPhrases() {
        String existingKeyword = "кота -накормить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);

        String phrase1 = "кота накормил";
        String phrase2 = "кота";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2)));
    }

    @Test
    public void execute_OneNewPhraseIsPartiallyAppendedByMinusToAvoidDuplicationInNewPhrases() {
        String existingKeyword = "кота накормить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);

        String phrase1 = "кота -накормил -погладил";
        String phrase2 = "кота погладил";
        String phrase3 = "кота";
        String phrase3Minus = "погладить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2),
                        isAddedWithMinus(phrase3, phrase3Minus)));
    }

    @Test
    public void execute_OneNewPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithExistingPhrases() {
        String existingKeyword = "кота -накормил -погладил";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingKeyword);

        String phrase1 = "кота накормить";
        String phrase2 = "кота погладить";
        String phrase3 = "кота";
        String phrase3Minus = "накормить";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2),
                        isAddedWithMinus(phrase3, phrase3Minus)));
    }

    // фразы, не подлежащие расклейке в одной группе

    @Test
    public void execute_TwoNewPhrasesNotSuitableForUngluing() {
        createOneActiveAdGroup();

        String phrase1 = "дикий кот";
        String phrase2 = "домашний кот";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isAdded(phrase2)));
    }

    @Test
    public void execute_NewAndExistingPhraseNotSuitableForUngluing() {
        String existingPhrase = "дикий кот";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "домашний пес";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
    }

    // сохранение

    @Test
    public void execute_UngluedNewPhraseIsSavedCorrectly() {
        String existingPhrase = "кот пес";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase1 = "кот слоны";
        String newPhrase2 = "кот";
        List<String> newPhrase2Minuses = asList("слон", "пес");
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isAdded(newPhrase1),
                        isAddedWithMinuses(newPhrase2, newPhrase2Minuses)));

        Keyword expectedNewKeyword2 = new Keyword()
                .withPhrase(resultPhrase(newPhrase2, newPhrase2Minuses))
                .withNormPhrase(newPhrase2)
                .withWordsCount(1);

        Keyword actualNewKeyword2 = getKeyword(result.get(1).getResult().getId());
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
        Long keyword2Id = createKeyword(adGroupInfo1, existingPhrase2).getId();

        String phrase1 = "купить кота";
        String phrase2 = "слон";
        String phrase3 = "кот";
        List<String> phrase3Minuses = asList("купить", "дикий");
        String phrase4 = "деревянный пони купить";
        String phrase5 = "саламандра";
        String phrase6 = "пони";
        String phrase6Minus = "деревянный";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3),
                clientKeyword(adGroupInfo1, phrase4),
                clientKeyword(adGroupInfo1, phrase5),
                clientKeyword(adGroupInfo1, phrase6));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase1),
                        isAdded(phrase2),
                        isAddedWithMinuses(phrase3, phrase3Minuses),
                        isAdded(phrase4),
                        isAdded(phrase5),
                        isAddedWithMinus(phrase6, phrase6Minus)));

        Keyword existingKeyword2 = getKeyword(keyword2Id);
        assertThat(existingKeyword2.getPhrase(), equalTo(resultPhrase(existingPhrase2, "купить")));

        Keyword newKeyword3 = getKeyword(result.get(2).getResult().getId());
        assertThat(newKeyword3.getPhrase(), equalTo(resultPhrase(phrase3, phrase3Minuses)));

        Keyword newKeyword6 = getKeyword(result.get(5).getResult().getId());
        assertThat(newKeyword6.getPhrase(), equalTo(resultPhrase(phrase6, phrase6Minus)));
    }
}
