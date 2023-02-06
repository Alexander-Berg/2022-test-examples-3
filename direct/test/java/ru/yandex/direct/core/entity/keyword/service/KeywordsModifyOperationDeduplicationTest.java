package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationResult;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.resultPhrase;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithMinus;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsModifyOperationDeduplicationTest extends KeywordsModifyOperationBaseTest {

    // дедупликация операции обновления и ее зависимость от других операций

    @Test
    public void execute_SimpleUpdateDeduplication() {
        String phraseForUpdate = "первая фраза";
        String phraseExisting = "вторая фраза";
        String phraseUntouched = "третья фраза";
        String adGroup2PhraseForUpdate = "фраза в другой группе";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        Long existingKeywordId = createKeyword(adGroupInfo1, phraseExisting).getId();
        createKeyword(adGroupInfo1, phraseUntouched);
        Long adGroup2KeywordIdForUpdate = createKeyword(adGroupInfo2, adGroup2PhraseForUpdate).getId();

        List<ModelChanges<Keyword>> updateList =
                asList(
                        keywordModelChanges(keywordIdForUpdate, phraseExisting),
                        keywordModelChanges(adGroup2KeywordIdForUpdate, phraseExisting));

        Result<KeywordsModificationResult> result = execute(null, updateList, null);
        assertResultIsSuccessful(result, null,
                asList(isNotUpdated(existingKeywordId, phraseExisting),
                        isUpdated(adGroup2KeywordIdForUpdate, phraseExisting)), null);
        assertExistingPhrases(phraseExisting, phraseExisting, phraseUntouched);
    }

    @Test
    public void execute_SimpleUpdateDeduplicationWithUnimportantDeletion() {
        String phraseForUpdate = "первая фраза";
        String phraseExisting = "вторая фраза";
        String phraseForDelete = "третья фраза";
        String adGroup2PhraseForUpdate = "фраза в другой группе";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        Long existingKeywordId = createKeyword(adGroupInfo1, phraseExisting).getId();
        Long adGroup1KeywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        Long adGroup2KeywordIdForUpdate = createKeyword(adGroupInfo2, adGroup2PhraseForUpdate).getId();

        List<ModelChanges<Keyword>> updateList =
                asList(
                        keywordModelChanges(keywordIdForUpdate, phraseExisting),
                        keywordModelChanges(adGroup2KeywordIdForUpdate, phraseExisting));
        List<Long> deleteList = singletonList(adGroup1KeywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(null, updateList, deleteList);
        assertResultIsSuccessful(result, null,
                asList(isNotUpdated(existingKeywordId, phraseExisting),
                        isUpdated(adGroup2KeywordIdForUpdate, phraseExisting)),
                singletonList(adGroup1KeywordIdForDelete));
        assertExistingPhrases(phraseExisting, phraseExisting);
    }

    @Test
    public void execute_DeletedKeywordIsNotConsideredByUpdateDeduplication() {
        String phraseForUpdate = "первая фраза";
        String phraseForDelete = "вторая фраза";
        String phraseUntouched = "нетронутая фраза";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        Long keywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForDelete));
        List<Long> deleteList = singletonList(keywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(null, updateList, deleteList);
        assertResultIsSuccessful(result, null, singletonList(isUpdated(keywordIdForUpdate, phraseForDelete)),
                singletonList(keywordIdForDelete));
        assertExistingPhrases(phraseForDelete, phraseUntouched);
    }

    @Test
    public void execute_AddedKeywordIsNotConsideredByUpdateDeduplication() {
        String phraseForUpdate = "первая фраза";
        String phraseNewValue = "новая фраза";
        String phraseUntouched = "нетронутая фраза";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseNewValue));
        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isNotAdded(phraseNewValue)),
                singletonList(isUpdated(keywordIdForUpdate, phraseNewValue)), null);
        assertExistingPhrases(phraseNewValue, phraseUntouched);
    }

    // дедупликация операции добавления и ее зависимость от других операций

    @Test
    public void execute_SimpleAddDeduplication() {
        String phraseExisting = "существующая фраза";
        String phraseUntouched = "другая фраза";
        String adGroup2PhraseUntouched = "фраза в другой группе";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, phraseExisting);
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseUntouched);

        List<Keyword> addList =
                asList(
                        clientKeyword(adGroupInfo1, phraseExisting),
                        clientKeyword(adGroupInfo2, phraseExisting));

        Result<KeywordsModificationResult> result = execute(addList, null, null);
        assertResultIsSuccessful(result,
                asList(isNotAdded(phraseExisting), isAdded(phraseExisting)), null, null);
        assertExistingPhrases(phraseExisting, phraseExisting,
                phraseUntouched, adGroup2PhraseUntouched);
    }

    @Test
    public void execute_SimpleAddDeduplicationWithUnimportantDeletion() {
        String phraseExisting = "существующая фраза";
        String phraseForDelete = "на удаление";
        String phraseUntouched = "другая фраза";
        String adGroup2PhraseUntouched = "фраза в другой группе";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, phraseExisting);
        Long adGroup1KeywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseUntouched);

        List<Keyword> addList =
                asList(
                        clientKeyword(adGroupInfo1, phraseExisting),
                        clientKeyword(adGroupInfo2, phraseExisting));
        List<Long> deleteList = singletonList(adGroup1KeywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, asList(isNotAdded(phraseExisting), isAdded(phraseExisting)),
                null, singletonList(adGroup1KeywordIdForDelete));
        assertExistingPhrases(phraseExisting, phraseExisting,
                phraseUntouched, adGroup2PhraseUntouched);
    }

    @Test
    public void execute_SimpleAddDeduplicationWithUnimportantUpdating() {
        String phraseExisting = "существующая фраза";
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "новое значение";
        String phraseUntouched = "другая фраза";
        String adGroup2PhraseUntouched = "фраза в другой группе";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, phraseExisting);
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseUntouched);

        List<Keyword> addList =
                asList(
                        clientKeyword(adGroupInfo1, phraseExisting),
                        clientKeyword(adGroupInfo2, phraseExisting));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, asList(isNotAdded(phraseExisting), isAdded(phraseExisting)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseExisting, phraseExisting,
                phraseForUpdateNewValue, phraseUntouched, adGroup2PhraseUntouched);
    }

    @Test
    public void execute_DeletedKeywordIsNotConsideredByAddDeduplication() {
        String phraseForDelete = "на удаление";
        String phraseUntouched = "другая фраза";
        createOneActiveAdGroup();
        Long keywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForDelete));
        List<Long> deleteList = singletonList(keywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForDelete)),
                null, singletonList(keywordIdForDelete));
        assertExistingPhrases(phraseForDelete, phraseUntouched);
    }

    @Test
    public void execute_OldValueOfUpdatedKeywordIsNotConsideredByAddDeduplication() {
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "новое значение";
        String phraseUntouched = "другая фраза";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForUpdate));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForUpdate)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForUpdate, phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_NewValueOfUpdatedKeywordIsConsideredByAddDeduplication() {
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "новое значение";
        String phraseUntouched = "другая фраза";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForUpdateNewValue));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isNotAdded(phraseForUpdateNewValue)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_NewUngluedValueOfUpdatedKeywordIsConsideredByAddDeduplication() {
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "фраза";
        String phraseForUpdateNewValueMinus = "другой";
        String phraseForAddValue = "фраза -другая";
        String phraseExisting = "другая фраза";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAddValue));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isNotAdded(phraseForAddValue)),
                singletonList(
                        isUpdatedWithMinus(keywordIdForUpdate, phraseForUpdateNewValue, phraseForUpdateNewValueMinus)),
                null);
        assertExistingPhrases(resultPhrase(phraseForUpdateNewValue, phraseForUpdateNewValueMinus), phraseExisting);
    }

    @Test
    public void execute_OldValueOfAffectedKeywordIsNotConsideredByAddDeduplication() {
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "другая фраза";
        String phraseExisting = "фраза";
        String phraseExistingMinus = "другой";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseExisting));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseExisting)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseExisting, phraseForUpdateNewValue,
                resultPhrase(phraseExisting, phraseExistingMinus));
    }

    @Test
    public void execute_NewValueOfAffectedKeywordIsConsideredByAddDeduplication() {
        String phraseForUpdate = "на обновление";
        String phraseForUpdateNewValue = "другая фраза";
        String phraseForAdd = "фраза -другая";
        String phraseExisting = "фраза";
        String phraseExistingMinus = "другой";
        createOneActiveAdGroup();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isNotAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForUpdateNewValue, resultPhrase(phraseExisting, phraseExistingMinus));
    }
}
