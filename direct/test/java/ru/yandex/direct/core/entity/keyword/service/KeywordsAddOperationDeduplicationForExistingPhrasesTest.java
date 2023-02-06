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
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAddedWithId;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * Тесты обработки дубликатов добавляемых фраз и существующих.
 * Например, когда в группе есть фраза "купить слон", а туда добавляется "слон купить".
 * Эти тесты не покрывают случай, когда добавляемые фразы одновременно
 * дублируются между собой и с существующими.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationDeduplicationForExistingPhrasesTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_OneNewPhraseThatIsNotDuplicateWithExisting_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_2)));
        assertThat(result.get(0).getResult().getId(), not(equalTo(existingKeywordId)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_OnePhraseThatIsDuplicateWithExistingPhraseInDifferentAdGroup_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo2, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertThat(result.get(0).getResult().getId(), not(equalTo(existingKeywordId)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhrase_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, existingPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase)));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhrase_ResultHasSourcePhraseInsteadOfExisting() {
        String existingPhrase = "каша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "каша каша";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), newPhrase)));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_PhrasesDifferByMinusWords_PhrasesAreComparedWithMinuses() {
        String existingPhrase = "каша -!ваша";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "каша -!наша";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_PhrasesDifferByOriginButEqualByNormalForm_PhrasesAreComparedByNormalForm() {
        String existingPhrase = "каша -!наша -!ваша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "каша не каша -!ваша -!наша";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), newPhrase)));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhraseAfterPrettifying_ResultHasCorrectInfo() {
        String existingPhrase = "каша наша -почти";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "каша  наша  -почти";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(
                isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase)));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhraseAfterStopwordsFixation_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "каша -наша";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isNotAddedWithFixation(existingPhrase, "-наша", "-!наша")));
        assertThat(result.get(0).getResult().getId(), equalTo(existingKeywordId));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_DuplicateWithExistingPhraseSimpleAndAfterPrettifyingAndStopwordsFixation_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "каша  -!наша";
        String newPhrase2 = "каша -наша";
        String newPhrase3 = "каша -!наша";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2),
                clientKeyword(adGroupInfo1, newPhrase3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase),
                        isNotAddedWithFixation(existingPhrase, "-наша", "-!наша"),
                        isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase)));
        assertThat(result.get(1).getResult().getId(), equalTo(existingKeywordId));
        checkValidationHasDuplicateInExistingWarnings(result, true, true, true);
    }

    /**
     * Дубликаты с существующими фразами не мешают добавляться уникальной фразе
     */
    @Test
    public void execute_UniquePhraseAndDuplicateWithExisting_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "каша  -!наша";
        String newPhrase2 = "уникальная фраза";
        String newPhrase3 = "каша -!наша";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2),
                clientKeyword(adGroupInfo1, newPhrase3));
        // сделаем разок full для спокойствия души
        MassResult<AddedKeywordInfo> result = executeFull(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase),
                        isAdded(newPhrase2),
                        isNotAddedWithId(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase)));
        assertThat(result.get(1).getResult().getId(), not(equalTo(existingKeywordId)));
        checkValidationHasDuplicateInExistingWarnings(result, true, false, true);
    }

    // связь подсчета дубликатов с проверкой ограничения максимального числа фраз на группу

    /**
     * В запросе есть уникальные фразы и есть дубликаты с существующими.
     * Валидацией отклонено добавление в группу из-за превышения ограничения на количество фраз.
     * Для всех фраз из запроса должны вернуться ошибки без предупреждений о дублировании.
     */
    @Test
    public void execute_DuplicateWithExistingPhrasesDeniedByMaxKeywordsValidation_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 198);
        createKeyword(adGroupInfo1, PHRASE_1);

        String phrase1 = "уникальная фраза";
        String phrase2 = "уникальнее первой";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessful(false, false, false));
        checkValidationHasDuplicateInExistingWarnings(result, false, false, false);
    }

    /**
     * В запросе есть уникальные фразы и есть дубликаты с существующими.
     * Вместе с дубликатами общее количество фраз превышает ограничение на количество фраз в группе,
     * но фразы должны успешно добавиться, так как дубликаты не должны учитываться.
     */
    @Test
    public void execute_DuplicateWithExistingPhrasesDoesNotCountedInMaxAdGroupLimit() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 197);
        createKeyword(adGroupInfo1, PHRASE_1);

        String phrase1 = "уникальная фраза";
        String phrase2 = "уникальнее первой";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isNotAdded(PHRASE_1), isAdded(phrase2)));
        checkValidationHasDuplicateInExistingWarnings(result, false, true, false);
    }

    /**
     * Посчитанные дубликаты между новыми и существующими фразами
     * затем отклонены валидацией из-за превышения максимального числа фраз на группу,
     * так как в группу добавлялась еще и уникальная фраза.
     * При этом в другую группу добавляется уникальная фраза,
     * которая должна добавиться несмотря на отклонение всех фраз в первой группе.
     */
    @Test
    public void execute_DuplicatesWithExistingPhrasesDeniedByMaxKeywordsValidationDoesntAffectOtherAdGroup_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();
        createKeywords(adGroupInfo1, 199);
        createKeyword(adGroupInfo1, PHRASE_1);

        String firstAdGroupPhrase1 = "уникальная фраза первой группы 1";
        String firstAdGroupPhrase2 = "уникальная фраза первой группы 2";
        String secondAdGroupPhrase = "уникальная фраза второй группы";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase1),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(null, isAdded(secondAdGroupPhrase), null, null));
        checkValidationHasDuplicateInNewWarnings(result, false, false, false, false);
    }

    // проверяем, что дубликаты реально не добавляются

    @Test
    public void execute_OneDuplicateWithExistingPhrase_PhraseIsNotAdded() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, existingPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assumeThat(result, isSuccessfulWithMatchers(isNotAdded(existingPhrase)));

        int clientKeywordsNumber = getClientKeywordsNumber();
        assertThat("клиенту не должна добавиться ключевая фраза",
                clientKeywordsNumber, equalTo(1));
    }

    @Test
    public void execute_UniquePhraseAndDuplicateWithExistingInDifferentAdGroups_ResultHasCorrectInfo() {
        String firstAdGroupExistingPhrase = "каша -!наша";
        String secondAdGroupExistingPhrase = "фраза как фраза";
        createTwoActiveAdGroups();
        Long firstAdGroupExistingKeywordId = createKeyword(adGroupInfo1, firstAdGroupExistingPhrase).getId();
        Long secondAdGroupExistingKeywordId = createKeyword(adGroupInfo2, secondAdGroupExistingPhrase).getId();

        String firstAdGroupPhrase1 = "каша  -!наша";
        String firstAdGroupPhrase2 = "уникальная фраза для первой группы";
        String secondAdGroupPhrase1 = "уникальная фраза для второй группы";
        String secondAdGroupPhrase2 = "как фраза";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, firstAdGroupPhrase1),
                clientKeyword(adGroupInfo1, firstAdGroupPhrase2),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase1),
                clientKeyword(adGroupInfo2, secondAdGroupPhrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAdded(firstAdGroupExistingPhrase),
                        isAdded(firstAdGroupPhrase2),
                        isAdded(secondAdGroupPhrase1),
                        isNotAdded(secondAdGroupPhrase2)));
        assertThat(result.get(0).getResult().getId(), equalTo(firstAdGroupExistingKeywordId));
        assertThat(result.get(3).getResult().getId(), equalTo(secondAdGroupExistingKeywordId));
        assertThat(result.get(1).getResult().getId(), not(equalTo(firstAdGroupExistingKeywordId)));
        assertThat(result.get(1).getResult().getId(), not(equalTo(secondAdGroupExistingKeywordId)));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(2).getResult().getId())));
        assertThat(result.get(2).getResult().getId(), not(equalTo(firstAdGroupExistingKeywordId)));
        assertThat(result.get(2).getResult().getId(), not(equalTo(secondAdGroupExistingKeywordId)));
        checkValidationHasDuplicateInExistingWarnings(result, true, false, false, true);

        int clientKeywordsNumber = getClientKeywordsNumber();
        assertThat("клиенту должны добавиться только уникальные ключевые фразы",
                clientKeywordsNumber, equalTo(4));
    }
}
