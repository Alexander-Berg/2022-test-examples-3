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
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * Тесты обработки дубликатов обновляемых фраз и других существующих.
 * Например, когда в группе есть две фразы "купить слон" и "купить коня". Обновив вторую фразы на "слон купить",
 * мы удаляем обновляемую фразу и заменяем ее ид на первую
 * Эти тесты не покрывают случай, когда обновляемые фразы одновременно
 * дублируются между собой и с существующими.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationDeduplicationForExistingPhrasesTest extends KeywordsUpdateOperationBaseTest {

    // дублирование обновляемых фраз с существующими

    @Test
    public void execute_UpdateOnePhrase_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_OnePhraseThatIsDuplicateWithExistingPhraseInDifferentAdGroup_ResultHasCorrectInfo() {
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhrase_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, existingPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, existingPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate);
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhrase_ResultHasSourcePhraseInsteadOfExisting() {
        String existingPhrase = "каша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "каша каша";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, newPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate);
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhraseAfterPrettifying_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "каша  -!наша";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, existingPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate);
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OneDuplicateWithExistingPhraseAfterStopwordsFixation_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "каша -наша";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(
                isNotUpdatedWithFixation(existingKeywordId, existingPhrase, "-наша", "-!наша")));
        checkKeywordsDeleted(keywordIdToUpdate);
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_DuplicateWithExistingPhraseSimpleAndAfterPrettifyingAndStopwordsFixation_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String newPhrase1 = "каша  -!наша";
        String newPhrase2 = "каша -наша";
        String newPhrase3 = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2),
                keywordModelChanges(keywordIdToUpdate3, newPhrase3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotUpdated(existingKeywordId, existingPhrase),
                        isNotUpdatedWithFixation(existingKeywordId, existingPhrase, "-наша", "-!наша"),
                        isNotUpdated(existingKeywordId, existingPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate1, keywordIdToUpdate2, keywordIdToUpdate3);
        checkValidationHasDuplicateInExistingWarnings(result, true, true, true);
    }

    @Test
    public void execute_UniquePhraseAndDuplicateWithExisting_ResultHasCorrectInfo() {
        String existingPhrase = "каша -!наша";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String newPhrase1 = "каша  -!наша";
        String newPhrase2 = "уникальная фраза";
        String newPhrase3 = "каша -!наша";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2),
                keywordModelChanges(keywordIdToUpdate3, newPhrase3));
        // сделаем разок full для спокойствия души
        MassResult<UpdatedKeywordInfo> result = executeFull(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotUpdated(existingKeywordId, existingPhrase),
                        isUpdated(keywordIdToUpdate2, newPhrase2),
                        isNotUpdated(existingKeywordId, existingPhrase)));
        checkKeywordsDeleted(keywordIdToUpdate1, keywordIdToUpdate3);
        checkValidationHasDuplicateInExistingWarnings(result, true, false, true);
    }

    @Test
    public void execute_UniquePhraseAndDuplicateWithExistingInDifferentAdGroups_ResultHasCorrectInfo() {
        String firstAdGroupExistingPhrase = "каша -!наша";
        String secondAdGroupExistingPhrase = "фраза как фраза";
        createTwoActiveAdGroups();
        Long firstAdGroupExistingKeywordId = createKeyword(adGroupInfo1, firstAdGroupExistingPhrase).getId();
        Long firstAdGroupKeywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long firstAdGroupKeywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();
        Long secondAdGroupExistingKeywordId = createKeyword(adGroupInfo2, secondAdGroupExistingPhrase).getId();
        Long secondAdGroupKeywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();
        Long secondAdGroupKeywordIdToUpdate3 = createKeyword(adGroupInfo2, PHRASE_3).getId();
        String firstAdGroupPhrase1 = "каша  -!наша";
        String firstAdGroupPhrase2 = "уникальная фраза для первой группы";
        String secondAdGroupPhrase1 = "уникальная фраза для второй группы";
        String secondAdGroupPhrase2 = "как фраза";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(firstAdGroupKeywordIdToUpdate2, firstAdGroupPhrase1),
                keywordModelChanges(firstAdGroupKeywordIdToUpdate3, firstAdGroupPhrase2),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate2, secondAdGroupPhrase1),
                keywordModelChanges(secondAdGroupKeywordIdToUpdate3, secondAdGroupPhrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotUpdated(firstAdGroupExistingKeywordId, firstAdGroupExistingPhrase),
                        isUpdated(firstAdGroupKeywordIdToUpdate3, firstAdGroupPhrase2),
                        isUpdated(secondAdGroupKeywordIdToUpdate2, secondAdGroupPhrase1),
                        isNotUpdated(secondAdGroupExistingKeywordId, secondAdGroupPhrase2)));
        checkKeywordsDeleted(firstAdGroupKeywordIdToUpdate2, secondAdGroupKeywordIdToUpdate3);
        checkValidationHasDuplicateInExistingWarnings(result, true, false, false, true);
    }
}
