package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.keywordAlreadySuspended;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.keywordNotSuspended;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.ILLEGAL_CHARACTERS;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.MINUS_WORD_DELETE_PLUS_WORD;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.NOT_SINGLE_MINUS_WORD;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationValidationTest extends KeywordsUpdateOperationBaseTest {

    private ModelChanges<Keyword> modelChanges;
    // отсутствие ошибок на всех стадиях валидации

    @Test
    public void execute_Partial_OneValidItem_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
    }

    @Test
    public void execute_Full_OneValidItem_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
    }

    @Test
    public void execute_Partial_TwoValidItemsInOneAdGroup_ResultIsOk() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));
    }

    @Test
    public void execute_Partial_TwoValidItemsInDifferentAdGroups_ResultIsOk() {
        createTwoActiveAdGroups();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PHRASE_2), keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));
    }

    @Test
    public void execute_Full_TwoValidItemsInDifferentAdGroups_ResultIsOk() {
        createTwoActiveAdGroups();
        createTwoActiveAdGroups();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PHRASE_2), keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));
    }

    // pre validation

    @Test
    public void execute_Partial_OneItemWithPreInvalidId_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(-1L, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("id")), MUST_BE_VALID_ID)));
    }

    @Test
    public void execute_Partial_OneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PREINVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PREINVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PREINVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    // validation

    @Test
    public void execute_OneNotParsedPhraseChangePrice_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = keywordSteps.createKeywordWithText(PREINVALID_PHRASE_1, adGroupInfo1).getId();

        ModelChanges<Keyword> changesKeyword =
                new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                        .process(BigDecimal.ONE, Keyword.PRICE);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_ExistingWrongPhraseWithMinusPhrase_ChangePrice_ResultHasInvalidItem() {
        String phrase = "купить слона -1 2 3";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = keywordSteps.createKeywordWithText(phrase, adGroupInfo1).getId();

        ModelChanges<Keyword> changesKeyword =
                new ModelChanges<>(keywordIdToUpdate, Keyword.class).process(BigDecimal.ONE, Keyword.PRICE);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        NOT_SINGLE_MINUS_WORD)));
    }

    @Test
    public void execute_Partial_OneItemWithInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_Partial_OneItemWithInvalidPrice_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2, Long.MAX_VALUE));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("price")),
                        SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX)));
    }

    @Test
    public void execute_Partial_OneItemWithInvalidPhraseAndInvalidPrice_ResultHasBothErrors() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1, Long.MAX_VALUE));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("price")),
                        SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithNotParsedPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = keywordSteps.createKeywordWithText(PREINVALID_PHRASE_1, adGroupInfo1).getId();

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                new ModelChanges<>(keywordIdToUpdate2, Keyword.class)
                        .process(BigDecimal.ONE, Keyword.PRICE));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithInvalidPhrase_OperationFailed() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    // interaction of validation stages

    /**
     * Тест на то, что результат валидации очищается от нод для валидных элементов,
     * что позволяет изменять исходные данные и валидировать снова.
     */
    @Test
    public void execute_Partial_OneValidItemWithFixedStopwordsInPhraseBetweenValidationStages_ResultIsOk() {
        String sourcePhrase = "будет зафиксировано стоп-слово -на";
        String destPhrase = "будет зафиксировано стоп-слово -!на";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(keywordIdToUpdate, sourcePhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, destPhrase, "-на", "-!на")));
    }

    /**
     * Тест на то, что результат валидации очищается от нод для валидных элементов,
     * что позволяет изменять исходные данные и валидировать снова.
     */
    @Test
    public void execute_Partial_OneItemWithInvalidPhraseChangedBetweenValidationStages_ResultIsOk() {
        String sourcePhrase = "будет зафиксировано стоп-слово -на -будет -зафиксировано -стоп -слово";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> keywords = singletonList(keywordModelChanges(keywordIdToUpdate, sourcePhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
    }

    /**
     * Тест на то, что модель с неправильной синтаксически фразой не уходит на второй этап валидации
     */
    @Test
    @SuppressWarnings("ConstantConditions")
    public void execute_Partial_OneValidAndOneItemWithPreInvalidPhraseAndInvalidPrice_ResultHasErrorOnlyForPhrase() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> keywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                        keywordModelChanges(keywordIdToUpdate2, PREINVALID_PHRASE_1, Long.MAX_VALUE));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), ILLEGAL_CHARACTERS)));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    /**
     * Тест на то, что несколько разных ключевых фраз могут получать ошибки на разных этапах валидации,
     * и при этом есть валидные объекты, которые нормально добавляются
     */
    @Test
    public void execute_Partial_OneValidItemAndOneWithPreInvalidPhraseAndOneWithInvalidPrice_ResultHasErrorOnlyForPhrase() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> keywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PREINVALID_PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1),
                keywordModelChanges(keywordIdToUpdate3, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(null, null, isUpdated(keywordIdToUpdate3, PHRASE_2)));
    }

    @Test
    public void preValidate_SuspendAlreadySuspended_WarningNotSuspendedKeyword() {
        createOneActiveAdGroup();
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1, PHRASE_1);

        suspendedModelChange(keywordInfo.getId(), true);

        keywordRepository.update(keywordInfo.getShard(),
                Collections.singletonList(modelChanges.applyTo(keywordInfo.getKeyword())));

        suspendedModelChange(keywordInfo.getId(), true);

        ValidationResult<List<ModelChanges<Keyword>>, Defect> vr = validationService.
                preValidate(singletonList(modelChanges), Collections.singleton(keywordInfo.getId()),
                        Collections.singleton(keywordInfo.getId()));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                keywordAlreadySuspended())));
    }

    @Test
    public void preValidate_ResumeNotSuspended_WarningNotSuspendedKeyword() {
        createOneActiveAdGroup();
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1, PHRASE_1);

        suspendedModelChange(keywordInfo.getId(), false);

        keywordRepository.update(keywordInfo.getShard(),
                Collections.singletonList(modelChanges.applyTo(keywordInfo.getKeyword())));

        suspendedModelChange(keywordInfo.getId(), false);

        ValidationResult<List<ModelChanges<Keyword>>, Defect> vr = validationService.
                preValidate(singletonList(modelChanges),
                        Collections.singleton(keywordInfo.getId()), Collections.emptySet());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                keywordNotSuspended())));
    }

    private void suspendedModelChange(Long id, boolean isSuspended) {
        modelChanges = new ModelChanges<>(id, Keyword.class);
        modelChanges.process(isSuspended, Keyword.IS_SUSPENDED);
    }
}
