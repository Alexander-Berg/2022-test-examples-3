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
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixation;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * Тесты обработки дубликатов среди обновляемых фраз, когда, к примеру,
 * в одной группе одновременно обновляются на "слон купить" и "купить слон".
 * Эти тесты не покрывают случай, когда обновляемые фразы одновременно
 * дублируются между собой и с существующими.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationDeduplicationInUpdatePhrasesTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_UpdatedTwoThatAreNotDuplicates_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_1),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false);
    }

    @Test
    public void execute_UpdatedTwoPhrasesThatAreDuplicatesInDifferentAdGroups_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_3),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_3),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false);
    }

    @Test
    public void execute_UpdatedTwoDuplicatePhrases_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_3),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_3),
                isNotUpdated(keywordIdToUpdate1, PHRASE_3)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true);
    }

    @Test
    public void execute_UpdatedTwoDuplicatePhrases_ResultHasSourcePhraseInsteadOfDuplicatedOne() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "каша";
        String phrase2 = "каша каша";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, phrase1),
                isNotUpdated(keywordIdToUpdate1, phrase2)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true);
    }

    @Test
    public void execute_UpdatedDuplicates_OnePreInvalidAndOneValid_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PREINVALID_PHRASE_1),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(null, isNotUpdated(keywordIdToUpdate1, PHRASE_1)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInExistingWarnings(result, false, true);
    }

    @Test
    public void execute_UpdatedDuplicates_OneInvalidAndOneValid_DuplicateNotAffectedByInvalidApplyChanges() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, INVALID_PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(null, isNotUpdated(keywordIdToUpdate1, PHRASE_1)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInExistingWarnings(result, false, true);
    }

    @Test
    public void execute_UpdatedTwoDuplicatePhrasesAfterPrettifying_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "каша  -!наша";
        String phrase2 = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, phrase2),
                        isNotUpdated(keywordIdToUpdate1, phrase2)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true);
    }

    @Test
    public void execute_UpdatedTwoDuplicatePhrasesAfterStopwordsFixation_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "каша -наша";
        String phrase2 = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(keywordIdToUpdate1, phrase2, "-наша", "-!наша"),
                        isNotUpdated(keywordIdToUpdate1, phrase2)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true);
    }

    /**
     * Обработка множества дубликатов одной фразы
     */
    @Test
    public void execute_UpdatedDuplicatePhrasesSimpleAndAfterStopwordsFixationAndPrettifying_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "каша -наша";
        String phrase2 = "каша  -наша";
        String phrase3 = "каша -!наша";
        String resultPhrase = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        // сделаем разок full для спокойствия души
        MassResult<UpdatedKeywordInfo> result = executeFull(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(keywordIdToUpdate1, resultPhrase, "-наша", "-!наша"),
                        isNotUpdatedWithFixation(keywordIdToUpdate1, resultPhrase, "-наша", "-!наша"),
                        isNotUpdated(keywordIdToUpdate1, resultPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate2, keywordIdToUpdate3);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true, true);
    }

    /**
     * Дубликаты среди обновляемых фраз не мешают обновиться уникальной фразе
     */
    @Test
    public void execute_UpdatedDuplicatePhrasesAndUniqueItem_ResultHasCorrectInfo() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String phrase1 = "каша -наша";
        String phrase2 = "уникальная фраза";
        String phrase3 = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2),
                keywordModelChanges(keywordIdToUpdate3, phrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(keywordIdToUpdate1, phrase3, "-наша", "-!наша"),
                        isUpdated(keywordIdToUpdate2, phrase2),
                        isNotUpdated(keywordIdToUpdate1, phrase3)));
        checkKeywordsDeleted(keywordIdToUpdate3);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false, true);
    }

    /**
     * Дубликаты среди обновляемых фраз и уникальные фразы в разных группах не мешают друг другу
     */
    @Test
    public void execute_UpdatedDuplicatePhrasesAndUniqueItemsInDifferentAdGroups_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();

        Long firstAdGroupKeywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long firstAdGroupKeywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long firstAdGroupKeywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();
        Long secondAdGroupKeywordIdToUpdate1 = createKeyword(adGroupInfo2, PHRASE_1).getId();
        Long secondAdGroupKeywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();
        Long secondAdGroupKeywordIdToUpdate3 = createKeyword(adGroupInfo2, PHRASE_3).getId();

        String firstAdGroupPhrase1 = "каша -наша";
        String firstAdGroupPhrase2 = "уникальная фраза для первой группы";
        String firstAdGroupPhrase3 = "каша -!наша";
        String secondAdGroupPhrase1 = "дубль 2";
        String secondAdGroupPhrase2 = "дубль 2";
        String secondAdGroupPhrase3 = "уникальная фраза для второй группы";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(firstAdGroupKeywordIdToUpdate1, firstAdGroupPhrase1),
                keywordModelChanges(firstAdGroupKeywordIdToUpdate2, firstAdGroupPhrase2),
                keywordModelChanges(firstAdGroupKeywordIdToUpdate3, firstAdGroupPhrase3),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate1, secondAdGroupPhrase1),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate2, secondAdGroupPhrase2),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate3, secondAdGroupPhrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(firstAdGroupKeywordIdToUpdate1, firstAdGroupPhrase3, "-наша", "-!наша"),
                        isUpdated(firstAdGroupKeywordIdToUpdate2, firstAdGroupPhrase2),
                        isNotUpdated(firstAdGroupKeywordIdToUpdate1, firstAdGroupPhrase3),
                        isUpdated(secondAdGroupKeywordIdToUpdate1, secondAdGroupPhrase1),
                        isNotUpdated(secondAdGroupKeywordIdToUpdate1, secondAdGroupPhrase2),
                        isUpdated(secondAdGroupKeywordIdToUpdate3, secondAdGroupPhrase3)));
        checkKeywordsDeleted(firstAdGroupKeywordIdToUpdate3, secondAdGroupKeywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false, true, false, true, false);
    }

}
