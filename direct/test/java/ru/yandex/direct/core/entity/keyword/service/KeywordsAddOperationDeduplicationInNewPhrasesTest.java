package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAddedWithFixation;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты обработки дубликатов среди новых фраз, когда, к примеру,
 * в одну группу одновременно добавляется "слон купить" и "купить слон".
 * Эти тесты не покрывают случай, когда добавляемые фразы одновременно
 * дублируются между собой и с существующими.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationDeduplicationInNewPhrasesTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_TwoNewPhrasesThatAreNotDuplicates_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));
        assertThat(result.get(0).getResult().getId(), not(equalTo(result.get(1).getResult().getId())));
        checkValidationHasDuplicateInNewWarnings(result, false, false);
    }

    @Test
    public void execute_TwoNewPhrasesThatAreDuplicatesInDifferentAdGroups_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_1)));
        assertThat(result.get(0).getResult().getId(), not(equalTo(result.get(1).getResult().getId())));
        checkValidationHasDuplicateInNewWarnings(result, false, false);
    }

    @Test
    public void execute_TwoNewDuplicatePhrases_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(1).getResult().getId()));
        checkValidationHasDuplicateInNewWarnings(result, false, true);
    }

    @Test
    public void execute_TwoNewDuplicatePhrases_ResultHasSourcePhraseInsteadOfDuplicatedOne() {
        createOneActiveAdGroup();

        String phrase1 = "каша";
        String phrase2 = "каша каша";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, phrase1), clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isNotAdded(phrase2)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(1).getResult().getId()));
        checkValidationHasDuplicateInNewWarnings(result, false, true);
    }

    @Test
    public void execute_PhrasesDifferByMinusWords_PhrasesAreComparedWithMinuses() {
        createOneActiveAdGroup();

        String phrase1 = "каша -!ваша";
        String phrase2 = "каша -!наша";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, phrase1), clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isAdded(phrase2)));
        assertThat(result.get(0).getResult().getId(), not(equalTo(result.get(1).getResult().getId())));
        checkValidationHasDuplicateInNewWarnings(result, false, false);
    }

    @Test
    public void execute_PhrasesDifferByOriginButEqualByNormalForm_PhrasesAreComparedByNormalForm() {
        createOneActiveAdGroup();

        String phrase1 = "каша -!ваша -!наша";
        String phrase2 = "каша не каша -!наша -!ваша";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, phrase1), clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isNotAdded(phrase2)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(1).getResult().getId()));
        checkValidationHasDuplicateInNewWarnings(result, false, true);
    }

    @Test
    public void execute_TwoNewDuplicatePhrasesAfterPrettifying_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        String phrase1 = "каша  наша  -почти";
        String phrase2 = "каша наша -почти";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, phrase1), clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(phrase2),
                        isNotAdded(phrase2)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(1).getResult().getId()));
        checkValidationHasDuplicateInNewWarnings(result, false, true);
    }

    @Test
    public void execute_TwoNewDuplicatePhrasesAfterStopwordsFixation_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        String phrase1 = "каша -наша";
        String phrase2 = "каша -!наша";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, phrase1), clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithFixation(phrase2, "-наша", "-!наша"),
                        isNotAdded(phrase2)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(1).getResult().getId()));
        checkValidationHasDuplicateInNewWarnings(result, false, true);
    }

    /**
     * Обработка множества дубликатов одной фразы
     */
    @Test
    public void execute_NewDuplicatePhrasesSimpleAndAfterStopwordsFixationAndPrettifying_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        String phrase1 = "каша -!наша";
        String phrase2 = "каша  -наша";
        String phrase3 = "каша -!наша";
        String phrase4 = "каша -наша";
        String resultPhrase = "каша -!наша";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3),
                clientKeyword(adGroupInfo1, phrase4));
        // сделаем разок full для спокойствия души
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        List<Long> allIds = mapList(result.getResult(), r -> r.getResult().getId());

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAdded(resultPhrase),
                        isNotAddedWithFixation(resultPhrase, "-наша", "-!наша"),
                        isNotAdded(resultPhrase),
                        isNotAddedWithFixation(resultPhrase, "-наша", "-!наша")));
        Long addedId = result.get(0).getResult().getId();
        assertThat(allIds, beanDiffer(asList(addedId, addedId, addedId, addedId)));
        checkValidationHasDuplicateInNewWarnings(result, false, true, true, true);
    }

    /**
     * Дубликаты среди новых фраз не мешают добавляться уникальной фразе
     */
    @Test
    public void execute_NewDuplicatePhrasesAndUniqueItem_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        String phrase1 = "каша -наша";
        String phrase2 = "уникальная фраза";
        String phrase3 = "каша -!наша";
        String phrase4 = "дубль 2";
        String phrase5 = "дубль 2";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2),
                clientKeyword(adGroupInfo1, phrase3),
                clientKeyword(adGroupInfo1, phrase4),
                clientKeyword(adGroupInfo1, phrase5));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithFixation(phrase3, "-наша", "-!наша"),
                        isAdded(phrase2),
                        isNotAdded(phrase3),
                        isAdded(phrase4),
                        isNotAdded(phrase5)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(2).getResult().getId()));
        assertThat(result.get(3).getResult().getId(), equalTo(result.get(4).getResult().getId()));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(0).getResult().getId())));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(3).getResult().getId())));
        checkValidationHasDuplicateInNewWarnings(result, false, false, true, false, true);
    }

    // связь подсчета дубликатов с проверкой ограничения максимального числа фраз на группу

    /**
     * В запросе есть только дубликаты, из которых должен добавиться только один.
     * Так как он не может быть добавлен из-за превышения ограничения, для всех
     * фраз из запроса должны вернуться ошибки без предупреждений о дублировании.
     */
    @Test
    public void execute_NewDuplicatePhrasesDeniedByMaxKeywordsValidation_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 200);

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessful(false, false));
        checkValidationHasDuplicateInNewWarnings(result, false, false);
    }

    /**
     * В запросе есть дубликаты и уникальная фраза. Так как они не могут быть добавлены
     * из-за превышения ограничения, для всех фраз из запроса должны вернуться ошибки
     * без предупреждений о дублировании.
     */
    @Test
    public void execute_NewDuplicatePhrasesAndUniquePhraseDeniedByMaxKeywordsValidation_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);

        String phrase = "уникальная фраза";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, phrase),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessful(false, false, false));
        checkValidationHasDuplicateInNewWarnings(result, false, false, false);
    }

    /**
     * В запросе есть дубликаты и уникальная фраза. Вместе с дубликатами общее количество
     * фраз превышает ограничение на количество фраз в группе, но фразы должны успешно
     * добавиться, так как дубликаты не должны учитываться.
     */
    @Test
    public void execute_DuplicatesInNewPhrasesDoesNotCountedInMaxAdGroupLimit() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 198);

        String phrase = "уникальная фраза";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, phrase),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(phrase), isNotAdded(PHRASE_1)));
        checkValidationHasDuplicateInNewWarnings(result, false, false, true);
    }

    /**
     * Посчитанные дубликаты среди новых фраз затем отклонены валидацией
     * из-за превышения максимального числа фраз на группу, при этом в той
     * же группе присутствует уникальная фраза и еще есть уникальная фраза
     * в другой группе, которая должна добавиться несмотря на отклонение
     * всех фраз в первой группе.
     */
    @Test
    public void execute_NewDuplicatePhrasesAndUniquePhraseDeniedByMaxKeywordsValidationDoesntAffectOtherAdGroup_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();
        createKeywords(adGroupInfo1, 199);

        String firstAdGroupPhrase = "уникальная фраза первой группы";
        String secondAdGroupPhrase = "уникальная фраза второй группы";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(null, isAdded(secondAdGroupPhrase), null, null));
        checkValidationHasDuplicateInNewWarnings(result, false, false, false, false);
    }

    // проверяем, что дубликаты реально не добавляются

    @Test
    public void execute_TwoNewDuplicatePhrases_OnlyOnePhraseAdded() {
        createOneActiveAdGroup();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));

        int clientKeywordsNumber = getClientKeywordsNumber();
        assertThat("клиенту должна добавиться только одна ключевая фраза",
                clientKeywordsNumber, equalTo(1));
    }

    /**
     * Дубликаты среди новых фраз и уникальные фразы в разных группах не мешают друг другу
     */
    @Test
    public void execute_NewDuplicatePhrasesAndUniqueItemsInDifferentAdGroups_ResultHasCorrectInfoAndAddedOnlyUniquePhrases() {
        createTwoActiveAdGroups();

        String firstAdGroupPhrase1 = "каша -наша";
        String firstAdGroupPhrase2 = "уникальная фраза для первой группы";
        String firstAdGroupPhrase3 = "каша -!наша";
        String secondAdGroupPhrase1 = "дубль 2";
        String secondAdGroupPhrase2 = "дубль 2";
        String secondAdGroupPhrase3 = "уникальная фраза для второй группы";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, firstAdGroupPhrase1),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase2),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase3),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase1),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase2),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithFixation(firstAdGroupPhrase3, "-наша", "-!наша"),
                        isAdded(firstAdGroupPhrase2),
                        isNotAdded(firstAdGroupPhrase3),
                        isAdded(secondAdGroupPhrase1),
                        isNotAdded(secondAdGroupPhrase2),
                        isAdded(secondAdGroupPhrase3)));
        assertThat(result.get(0).getResult().getId(), equalTo(result.get(2).getResult().getId()));
        assertThat(result.get(3).getResult().getId(), equalTo(result.get(4).getResult().getId()));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(0).getResult().getId())));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(3).getResult().getId())));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(5).getResult().getId())));
        assertThat(result.get(5).getResult().getId(), not(equalTo(result.get(0).getResult().getId())));
        assertThat(result.get(5).getResult().getId(), not(equalTo(result.get(3).getResult().getId())));
        checkValidationHasDuplicateInNewWarnings(result, false, false, true, false, true, false);

        int clientKeywordsNumber = getClientKeywordsNumber();
        assertThat("клиенту должны добавиться только уникальные ключевые фразы",
                clientKeywordsNumber, equalTo(4));
    }
}
