package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds.Gen.AUTOTARGETING_PREFIX_IS_NOT_ALLOWED;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds.Gen.NOT_ACCEPTABLE_AD_GROUP_TYPE;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds.Keyword.MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.ILLEGAL_CHARACTERS;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.String.MINUS_WORD_DELETE_PLUS_WORD;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationValidationTest extends KeywordsAddOperationBaseTest {
    // отсутствие ошибок на всех стадиях валидации

    @Test
    public void execute_Partial_OneValidItem_ResultIsOk() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneNotParsedExisting_ResultIsOk() {
        createOneActiveAdGroup();
        keywordSteps.createKeywordWithText(PREINVALID_PHRASE_1, adGroupInfo1).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_Full_OneValidItem_ResultIsOk() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_Partial_TwoValidItemsInOneAdGroup_ResultIsOk() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));
    }

    @Test
    public void execute_Partial_TwoValidItemsInDifferentAdGroups_ResultIsOk() {
        createTwoActiveAdGroups();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));
    }

    @Test
    public void execute_Full_TwoValidItemsInDifferentAdGroups_ResultIsOk() {
        createTwoActiveAdGroups();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));
    }

    // pre validation

    @Test
    public void execute_AutotargetingFeatureNotAllowed_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1).withIsAutotargeting(true));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("isAutotargeting")),
                        AUTOTARGETING_PREFIX_IS_NOT_ALLOWED)));
    }

    @Test
    public void execute_Partial_OneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithPreInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), ILLEGAL_CHARACTERS)));
    }

    // validation

    @Test
    public void execute_Partial_OneItemWithInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_Partial_OneItemWithInvalidPrice_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1).withPrice(decimal(-1)));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("price")),
                        SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN)));
    }

    @Test
    public void execute_Partial_OneItemWithInvalidPhraseAndInvalidPrice_ResultHasBothErrors() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1).withPrice(decimal(-1)));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("price")),
                        SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithInvalidPhrase_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithInvalidPhrase_OperationFailed() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        MINUS_WORD_DELETE_PLUS_WORD)));
    }

    // post validation: items per ad group number

    @Test
    public void execute_Partial_OneValidItemAndKeywordPerAdGroupHasUpdatedLimitInDb_ResultHasInvalidItem() {
        ClientLimits clientLimits = new ClientLimits();
        clientLimits.withKeywordsCountLimit(1L);

        createOneActiveAdGroup(clientLimits);

        createKeywords(adGroupInfo1, 1);
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
    }

    @Test
    public void execute_Partial_OneValidItemAndKeywordsPerAdGroupExceeded_ResultHasInvalidItem() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 200);
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
    }

    @Test
    public void execute_Partial_TwoValidItemsAndKeywordsPerAdGroupExceeded_ResultHasInvalidItems() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false, false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED)));
    }

    @Test
    public void execute_Partial_OneAdGroupHasTooMuchElementsAndSecondIsOk_ResultHasInvalidItems() {
        createTwoActiveAdGroups();
        createKeywords(adGroupInfo1, 199);
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_2),
                clientKeyword(adGroupInfo2, PHRASE_3));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(null, null, isAdded(PHRASE_3)));
    }

    @Test
    public void execute_Full_OneAdGroupHasTooMuchElementsAndSecondIsOk_OperationIsFailed() {
        createTwoActiveAdGroups();
        createKeywords(adGroupInfo1, 199);
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_2),
                clientKeyword(adGroupInfo2, PHRASE_3));
        MassResult<AddedKeywordInfo> result = executeFull(keywords);
        assertThat(result, isSuccessfulWithMatchers(null, null, equalTo(null)));
    }

    @Test
    public void execute_Full_CpmYndxFrontpageAdGroupHasKeywords_ResultHasInvalidItems() {
        AdGroupInfo cpmYndxFrontpageAdGroupInfo = adGroupSteps.createDefaultCpmYndxFrontpageAdGroup(clientInfo);
        List<Keyword> keywords = asList(clientKeyword(cpmYndxFrontpageAdGroupInfo, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(Keyword.AD_GROUP_ID.name())),
                        NOT_ACCEPTABLE_AD_GROUP_TYPE)));
    }

    @Test
    public void execute_Partial_InvalidItemsAreNotCountedInMaxKeywordsCheck() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(null, isAdded(PHRASE_1)));
    }

    @Test
    public void execute_Partial_NewDuplicatedItemsAreNotCountedInMaxKeywordsCheck() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));
    }

    @Test
    public void execute_Partial_ExistingDuplicatedItemsAreNotCountedInMaxKeywordsCheck() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 199);
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isNotAdded(PHRASE_1)));
        assertThat(result.get(0).getResult().getId(), equalTo(existingKeywordId));
    }

    @Test
    public void execute_Partial_ExistingNotParsedItemNotCountedInMaxKeywordsCheck() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 198);
        keywordSteps.createKeywordWithText(PREINVALID_PHRASE_1, adGroupInfo1).getId();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    // взаимодействие этапов валидации

    /**
     * Тест на то, что результат валидации очищается от нод для валидных элементов,
     * что позволяет изменять исходные данные и валидировать снова.
     */
    @Test
    public void execute_OneValidItemWithFixedStopwordsBetweenValidationStages_WorksFine() {
        String sourcePhrase = "будет зафиксировано стоп-слово -на";
        String destPhrase = "будет зафиксировано стоп-слово -!на";
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, sourcePhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(destPhrase, "-на", "-!на")));
    }

    /**
     * Тест на то, что операция не фиксирует стоп-слова во фразе, не прошедшей предварителную валидацию
     */
    public void execute_OneOfTwoItemsHasPreInvalidPhraseChangedBetweenValidationStages_OperationDoesNotModifyInvalidKeyword() {
        String sourcePhrase = "слово -слово -на";
        createOneActiveAdGroup();
        List<Keyword> keywords =
                asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, sourcePhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));
        assertThat("стоп-слова не должны фиксироваться во фразе, которая имеет ошибки предварительной валидации",
                keywords.get(0).getPhrase(), equalTo(sourcePhrase));
    }

    /**
     * Тест на то, что модель с неправильной синтаксически фразой не уходит на второй этап валидации
     */
    @Test
    @SuppressWarnings("ConstantConditions")
    public void execute_Partial_OneOfTwoItemsHasPreInvalidPhraseAndInvalidPrice_KeywordWithPreInvalidPhraseIsNotSentToMainValidation() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1).withPrice(decimal(-1)),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessful(false, true));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")), ILLEGAL_CHARACTERS)));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    /**
     * Тест на то, что несколько разных ключевых фраз могут получать ошибки на разных этапах валидации,
     * и при этом есть валидные объекты, которые нормально добавляются
     */
    @Test
    public void execute_Partial_OneValidItemAndOneWithPreInvalidPhraseAndOneWithInvalidPrice_ResultHasErrorOnlyForPhrase() {
        createOneActiveAdGroup();
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PREINVALID_PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_1).withPrice(decimal(-1)),
                clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(null, null, isAdded(PHRASE_2)));
    }

    /**
     * При добавлении в режиме {@code autoPrices}, у фразы может не быть ставок.
     */
    @Test
    public void execute_FullWithAutoPrices_KeywordsWithoutPrices_success() {
        createOneActiveAdGroupAutoStrategy();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1)
                .withAutobudgetPriority(null)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        KeywordsAddOperation operation =
                createOperationWithAutoPrices(keywords, null, fixedAutoPrices, null);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    private BigDecimal decimal(long value) {
        return BigDecimal.valueOf(value);
    }
}
