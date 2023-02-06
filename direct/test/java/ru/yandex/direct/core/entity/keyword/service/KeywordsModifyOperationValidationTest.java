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
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.ADD_LIST_NAME;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.DELETE_LIST_NAME;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.UPDATE_LIST_NAME;
import static ru.yandex.direct.core.entity.keyword.model.Keyword.ID;
import static ru.yandex.direct.core.entity.keyword.model.Keyword.PHRASE;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds.Keyword.MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.ILLEGAL_CHARACTERS;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.MINUS_WORD_DELETE_PLUS_WORD;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsModifyOperationValidationTest extends KeywordsModifyOperationBaseTest {

    @Test(expected = IllegalArgumentException.class)
    public void execute_EmptyInput_ThrowsException() {
        createOneActiveAdGroup();
        executeAdd(null);
    }

    // операции по отдельности, данные валидны

    @Test
    public void execute_ValidAdd_ResultIsOk() {
        createOneActiveAdGroup();
        List<Keyword> addList = singletonList(validClientKeyword1(adGroupInfo1));

        Result<KeywordsModificationResult> result = executeAdd(addList);
        assertAddResultIsSuccessful(result, singletonList(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_ValidUpdate_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();

        List<ModelChanges<Keyword>> updateList = singletonList(validModelChanges1(keywordIdToUpdate));

        Result<KeywordsModificationResult> result = executeUpdate(updateList);
        assertUpdateResultIsSuccessful(result, singletonList(isUpdated(keywordIdToUpdate, NEW_PHRASE_1)));
    }

    @Test
    public void execute_ValidDelete_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();

        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = executeDelete(deleteList);
        assertDeleteResultIsSuccessful(result, singletonList(keywordIdToDelete));
    }

    // комбинации операций, данные валидны

    @Test
    public void execute_ValidAddAndUpdate_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(validClientKeyword3(adGroupInfo1));
        List<ModelChanges<Keyword>> updateList = singletonList(validModelChanges1(keywordIdToUpdate));

        Result<KeywordsModificationResult> result = execute(addList, updateList, null);
        assertResultIsSuccessful(result, singletonList(isAdded(PHRASE_3)),
                singletonList(isUpdated(keywordIdToUpdate, NEW_PHRASE_1)), null);
    }

    @Test
    public void execute_ValidAddAndDelete_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(validClientKeyword3(adGroupInfo1));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(PHRASE_3)),
                null, singletonList(keywordIdToDelete));
    }

    @Test
    public void execute_ValidUpdateAndDelete_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToDelete = createValidKeyword2(adGroupInfo1).getId();

        List<ModelChanges<Keyword>> updateList = singletonList(validModelChanges1(keywordIdToUpdate));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(null, updateList, deleteList);
        assertResultIsSuccessful(result, null, singletonList(isUpdated(keywordIdToUpdate, NEW_PHRASE_1)),
                singletonList(keywordIdToDelete));
    }

    @Test
    public void execute_ValidAddAndUpdateAndDelete_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToDelete = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(validClientKeyword3(adGroupInfo1));
        List<ModelChanges<Keyword>> updateList = singletonList(validModelChanges1(keywordIdToUpdate));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(PHRASE_3)),
                singletonList(isUpdated(keywordIdToUpdate, NEW_PHRASE_1)), singletonList(keywordIdToDelete));
    }

    // операции по отдельности, данные не валидны

    @Test
    public void execute_OnlyAddWithInvalidItem_ResultIsFailed() {
        createOneActiveAdGroup();
        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1));

        Result<KeywordsModificationResult> result = executeAdd(addList);
        assertResultIsFailed(result);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_OnlyAddWithValidAndInvalidItems_ResultIsFailed() {
        createOneActiveAdGroup();
        List<Keyword> addList = asList(
                validClientKeyword1(adGroupInfo1),
                clientKeyword(adGroupInfo1, INVALID_PHRASE_1));

        Result<KeywordsModificationResult> result = executeAdd(addList);
        assertResultIsFailed(result);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(1), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_OnlyUpdateWithInvalidItem_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1));

        Result<KeywordsModificationResult> result = executeUpdate(updateList);
        assertResultIsFailed(result, PHRASE_1);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(UPDATE_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_OnlyUpdateWithValidAndInvalidItems_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToUpdate2 = createValidKeyword2(adGroupInfo1).getId();

        List<ModelChanges<Keyword>> updateList = asList(
                validModelChanges1(keywordIdToUpdate1),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));

        Result<KeywordsModificationResult> result = executeUpdate(updateList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(UPDATE_LIST_NAME), index(1), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_OnlyDeleteWithInvalidItem_ResultIsFailed() {
        createOneActiveAdGroup();
        createValidKeyword1(adGroupInfo1);

        List<Long> deleteList = singletonList(UNEXISTENT_PHRASE_ID);

        Result<KeywordsModificationResult> result = executeDelete(deleteList);
        assertResultIsFailed(result, PHRASE_1);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(DELETE_LIST_NAME), index(0)),
                        OBJECT_NOT_FOUND)));
    }

    @Test
    public void execute_OnlyDeleteWithValidAndInvalidItems_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();

        List<Long> deleteList = asList(keywordIdToDelete, UNEXISTENT_PHRASE_ID);

        Result<KeywordsModificationResult> result = executeDelete(deleteList);
        assertResultIsFailed(result, PHRASE_1);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(DELETE_LIST_NAME), index(1)),
                        OBJECT_NOT_FOUND)));
    }

    // операции вместе, данные для некоторых из них не валидны

    @Test
    public void execute_InvalidDeleteAndOthersAreValid_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createValidKeyword1(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(validClientKeyword3(adGroupInfo1));
        List<ModelChanges<Keyword>> updateList = singletonList(validModelChanges1(keywordIdToUpdate));
        List<Long> deleteList = singletonList(UNEXISTENT_PHRASE_ID);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(DELETE_LIST_NAME), index(0)),
                        OBJECT_NOT_FOUND)));
    }

    @Test
    public void execute_InvalidUpdateAndOthersAreValid_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToUpdate = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(validClientKeyword3(adGroupInfo1));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(UPDATE_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_InvalidAddAndOthersAreValid_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToUpdate = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        List<ModelChanges<Keyword>> updateList =
                singletonList(validModelChanges1(keywordIdToUpdate));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_InvalidDeleteAndAddButUpdateIsValid_ResultIsFailed() {
        createOneActiveAdGroup();
        createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToUpdate = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        List<ModelChanges<Keyword>> updateList =
                singletonList(validModelChanges1(keywordIdToUpdate));
        List<Long> deleteList = singletonList(UNEXISTENT_PHRASE_ID);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(DELETE_LIST_NAME), index(0)),
                        OBJECT_NOT_FOUND)));
    }

    @Test
    public void execute_InvalidUpdateAndAddButDeleteIsValid_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();
        Long keywordIdToUpdate = createValidKeyword2(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1));
        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(UPDATE_LIST_NAME), index(0), field(PHRASE.name())),
                        MINUS_WORD_DELETE_PLUS_WORD)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(0), field(PHRASE.name())),
                        ILLEGAL_CHARACTERS)));
    }

    // валидация попытки обновления удаляемой фразы

    @Test
    public void execute_TryToUpdateDeletedPhrase_ResultIsFailed() {
        createOneActiveAdGroup();
        Long keywordIdToDeleteAndUpdate = createValidKeyword1(adGroupInfo1).getId();
        createValidKeyword2(adGroupInfo1).getId();

        List<ModelChanges<Keyword>> updateList =
                singletonList(keywordModelChanges(keywordIdToDeleteAndUpdate, PHRASE_3));
        List<Long> deleteList = singletonList(keywordIdToDeleteAndUpdate);

        Result<KeywordsModificationResult> result = execute(null, updateList, deleteList);
        assertResultIsFailed(result, PHRASE_1, PHRASE_2);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(UPDATE_LIST_NAME), index(0), field(ID.name())),
                        DefectIds.OBJECT_NOT_FOUND)));
    }

    // валидация максимального числа фраз на группу с учетом удаляемых

    @Test
    public void execute_DeletedPhrasesIsConsideredInAddValidationOfMaxAmountPerAdGroup() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();

        List<Keyword> addList = singletonList(clientKeyword(adGroupInfo1, PHRASE_2));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertResultIsSuccessful(result, singletonList(isAdded(PHRASE_2)), null,
                singletonList(keywordIdToDelete));
    }

    @Test
    public void execute_MaximumExceeded_ResultIsFailed() {
        createTwoActiveAdGroups();
        createKeywords(adGroupInfo1, 199);
        Long keywordIdToDelete = createValidKeyword1(adGroupInfo1).getId();
        createValidKeyword1(adGroupInfo2);

        List<Keyword> addList = asList(
                clientKeyword(adGroupInfo1, PHRASE_2),
                clientKeyword(adGroupInfo1, PHRASE_3),
                clientKeyword(adGroupInfo2, PHRASE_2));
        List<Long> deleteList = singletonList(keywordIdToDelete);

        Result<KeywordsModificationResult> result = execute(addList, null, deleteList);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getResult(), nullValue());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(0)),
                        MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(field(ADD_LIST_NAME), index(1)),
                        MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
        //noinspection ConstantConditions
        assertThat("ошибки должны быть только на двух добавляемых в первую группу фразах",
                result.getValidationResult().flattenErrors(), hasSize(2));
    }
}
