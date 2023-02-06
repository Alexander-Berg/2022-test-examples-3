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
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithMinus;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.resultPhrase;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithMinus;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsModifyOperationUngluingTest extends KeywordsModifyOperationBaseTest {

    // расклейка операции обновления и ее зависимость от других операций

    @Test
    public void execute_SimpleUpdateUngluing() {
        String phraseForUpdate = "rwlenj";
        String phraseForUpdateNewValue = "фраза";
        String phraseForUpdateNewValueMinus = "другой";
        String phraseExisting = "другая фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        String adGroup2PhraseExisting = "фраза группа2";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseExisting).getId();

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(null, updateList, null);
        assertResultIsSuccessful(result, null,
                singletonList(
                        isUpdatedWithMinus(keywordIdForUpdate, phraseForUpdateNewValue, phraseForUpdateNewValueMinus)),
                null);
        assertExistingPhrases(phraseExisting, resultPhrase(phraseForUpdateNewValue, phraseForUpdateNewValueMinus),
                phraseUntouched, adGroup2PhraseExisting);
    }

    @Test
    public void execute_SimpleUpdateUngluingOfExistingPhrase() {
        String phraseForUpdate = "rwlenj";
        String phraseForUpdateNewValue = "новая фраза";
        String phraseExisting = "фраза";
        String phraseExistingMinus = "новый";
        String phraseUntouched = "третья фраза здесь просто полежит";
        String adGroup2PhraseExisting = "фраза группа2";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseExisting);

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(null, updateList, null);
        assertResultIsSuccessful(result, null, singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)),
                null);
        assertExistingPhrases(phraseForUpdateNewValue, resultPhrase(phraseExisting, phraseExistingMinus),
                phraseUntouched, adGroup2PhraseExisting);
    }

    @Test
    public void execute_DeletedKeywordIsNotConsideredByUpdateUngluing() {
        String phraseForUpdate = "rwlenj";
        String phraseForUpdateNewValue = "фраза";
        String phraseForDelete = "другая фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        Long keywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));
        List<Long> deleteList = singletonList(keywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(null, updateList, deleteList);
        assertResultIsSuccessful(result, null, singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)),
                singletonList(keywordIdForDelete));
        assertExistingPhrases(phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_AddedKeywordIsNotConsideredByUpdateUngluing() {
        String phraseForUpdate = "rwlenj";
        String phraseForUpdateNewValue = "фраза";
        String phraseForUpdateNewValueMinus = "другой";
        String phraseForAdd = "другая фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForAdd,
                resultPhrase(phraseForUpdateNewValue, phraseForUpdateNewValueMinus), phraseUntouched);
    }

    // расклейка операции добавления и ее зависимость от других операций

    @Test
    public void execute_SimpleAddUngluing() {
        String phraseForAdd = "фраза";
        String phraseForAddMinus = "другой";
        String phraseExisting = "другая фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        String adGroup2PhraseExisting = "фраза группа2";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, phraseExisting);
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));

        Result<KeywordsModificationResult> result = execute(addList, null, null);
        assertResultIsSuccessful(result,
                singletonList(isAddedWithMinus(phraseForAdd, phraseForAddMinus)), null, null);
        assertExistingPhrases(resultPhrase(phraseForAdd, phraseForAddMinus), phraseExisting,
                phraseUntouched, adGroup2PhraseExisting);
    }

    @Test
    public void execute_SimpleAddUngluingOfExistingPhrase() {
        String phraseForAdd = "новая фраза";
        String phraseExisting = "фраза";
        String phraseExistingMinus = "новый";
        String phraseUntouched = "третья фраза здесь просто полежит";
        String adGroup2PhraseExisting = "фраза группа2";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo1, phraseExisting);
        createKeyword(adGroupInfo1, phraseUntouched);
        createKeyword(adGroupInfo2, adGroup2PhraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));

        Result<KeywordsModificationResult> result = execute(addList, null, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)), null, null);
        assertExistingPhrases(phraseForAdd, resultPhrase(phraseExisting, phraseExistingMinus),
                phraseUntouched, adGroup2PhraseExisting);
    }

    @Test
    public void execute_DeletedKeywordIsNotConsideredByAddUngluing() {
        String phraseForAdd = "фраза";
        String phraseForDelete = "другая фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<Long> deleteList = singletonList(keywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)), null,
                singletonList(keywordIdForDelete));
        assertExistingPhrases(phraseForAdd, phraseUntouched);
    }

    @Test
    public void execute_DeletedKeywordIsNotConsideredByAddUngluingOfExistingPhrase() {
        String phraseForAdd = "новая фраза";
        String phraseForDelete = "фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForDelete = createKeyword(adGroupInfo1, phraseForDelete).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<Long> deleteList = singletonList(keywordIdForDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)), null,
                singletonList(keywordIdForDelete));
        assertExistingPhrases(phraseForAdd, phraseUntouched);
    }

    @Test
    public void execute_OldValueOfUpdatedKeywordIsNotConsideredByAddUngluing() {
        String phraseForAdd = "фраза";
        String phraseForUpdate = "старая фраза";
        String phraseForUpdateNewValue = "обновленная фраза ололо";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForAdd, phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_OldValueOfUpdatedKeywordIsNotConsideredByAddUngluingOfExistingPhrase() {
        String phraseForAdd = "новая фраза";
        String phraseForUpdate = "фраза";
        String phraseForUpdateNewValue = "обновленная фраза ололо";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForAdd, phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_NewValueOfUpdatedKeywordIsConsideredByAddUngluing() {
        String phraseForAdd = "фраза";
        String phraseForAddMinus = "обновлять";
        String phraseForUpdate = "старая фраза";
        String phraseForUpdateNewValue = "обновленная фраза";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAddedWithMinus(phraseForAdd, phraseForAddMinus)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(resultPhrase(phraseForAdd, phraseForAddMinus),
                phraseForUpdateNewValue, phraseUntouched);
    }

    @Test
    public void execute_NewValueOfUpdatedKeywordCanBeUngluedByAdd() {
        String phraseForAdd = "новая фраза";
        String phraseForUpdate = "старая фраза";
        String phraseForUpdateNewValue = "фраза";
        String phraseForUpdateNewValueMinus = "новый";
        String phraseUntouched = "третья фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForAdd,
                resultPhrase(phraseForUpdateNewValue, phraseForUpdateNewValueMinus), phraseUntouched);
    }

    @Test
    public void execute_UngluedAffectedKeywordCanBeUsedByAddUngluing() {
        String phraseForAdd = "фраза";
        String phraseForAddMinus = "ололо";
        String phraseForUpdate = "старая фраза";
        String phraseForUpdateNewValue = "обновленная фраза ололо";
        String phraseExisting = "фраза ололо";
        String phraseExistingMinus = "обновлять";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAddedWithMinus(phraseForAdd, phraseForAddMinus)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(resultPhrase(phraseForAdd, phraseForAddMinus), phraseForUpdateNewValue,
                resultPhrase(phraseExisting, phraseExistingMinus));
    }

    @Test
    public void execute_UngluedAffectedKeywordCanBeUngluedByAdd() {
        String phraseForAdd = "новая фраза";
        String phraseForUpdate = "старая фраза";
        String phraseForUpdateNewValue = "обновленная фраза";
        String phraseExisting = "фраза";
        List<String> phraseExistingMinuses = asList("обновлять", "новый");
        createTwoActiveAdGroups();
        Long keywordIdForUpdate = createKeyword(adGroupInfo1, phraseForUpdate).getId();
        createKeyword(adGroupInfo1, phraseExisting);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdForUpdate, phraseForUpdateNewValue));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                singletonList(isUpdated(keywordIdForUpdate, phraseForUpdateNewValue)), null);
        assertExistingPhrases(phraseForAdd, phraseForUpdateNewValue,
                resultPhrase(phraseExisting, phraseExistingMinuses));
    }

    @Test
    public void execute_AddUngluingWorksWellWithUpdateDeduplication() {
        String phraseForAdd = "фраза";
        String phraseForAddMinus = "обновлять";
        String phraseForUpdate1 = "старая фраза 1";
        String phraseForUpdate2 = "старая фраза 2";
        String phraseForUpdateNewValue1 = "обновленная фраза";
        String phraseForUpdateNewValue2 = "фраза обновленная";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate1 = createKeyword(adGroupInfo1, phraseForUpdate1).getId();
        Long keywordIdForUpdate2 = createKeyword(adGroupInfo1, phraseForUpdate2).getId();

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                asList(
                        keywordModelChanges(keywordIdForUpdate1, phraseForUpdateNewValue1),
                        keywordModelChanges(keywordIdForUpdate2, phraseForUpdateNewValue2));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAddedWithMinus(phraseForAdd, phraseForAddMinus)),
                asList(isUpdated(keywordIdForUpdate1, phraseForUpdateNewValue1),
                        isNotUpdated(keywordIdForUpdate1, phraseForUpdateNewValue2)), null);
        assertExistingPhrases(resultPhrase(phraseForAdd, phraseForAddMinus), phraseForUpdateNewValue1);
    }

    @Test
    public void execute_AddUngluingOfExistingPhraseWorksWellWithUpdateDeduplication() {
        String phraseForAdd = "фраза обновленная черепашкой";
        String phraseForUpdate1 = "старая фраза 1";
        String phraseForUpdate1Minus = "черепашка";
        String phraseForUpdate2 = "старая фраза 2";
        String phraseForUpdateNewValue1 = "обновленная фраза";
        String phraseForUpdateNewValue2 = "фраза обновленная";
        String phraseUntouched = "эта фраза здесь просто полежит";
        createTwoActiveAdGroups();
        Long keywordIdForUpdate1 = createKeyword(adGroupInfo1, phraseForUpdate1).getId();
        Long keywordIdForUpdate2 = createKeyword(adGroupInfo1, phraseForUpdate2).getId();
        createKeyword(adGroupInfo1, phraseUntouched);

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, phraseForAdd));
        List<ModelChanges<Keyword>> updateList =
                asList(
                        keywordModelChanges(keywordIdForUpdate1, phraseForUpdateNewValue1),
                        keywordModelChanges(keywordIdForUpdate2, phraseForUpdateNewValue2));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(phraseForAdd)),
                asList(isUpdated(keywordIdForUpdate1, phraseForUpdateNewValue1),
                        isNotUpdated(keywordIdForUpdate1, phraseForUpdateNewValue2)), null);
        assertExistingPhrases(phraseForAdd, resultPhrase(phraseForUpdateNewValue1, phraseForUpdate1Minus),
                phraseUntouched);
    }
}
