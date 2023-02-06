package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AffectedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixation;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationUngluingOfOtherExistingPhrasesTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByMinusOfUpdatedPhraseInNormalForm() {
        String existingPhrase = "кота";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    @Test
    public void execute_UnglueWontRunIfItIsNotEnabled() {
        String existingPhrase = "кота";
        createOneActiveAdGroup();

        createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperationWithoutUnglue(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    // игнорирование регистра при расклейке

    @Test
    public void execute_OneOtherExistingPhraseWithUpperCaseIsAppendedByMinusOfUpdatedPhraseInNormalForm() {
        String existingPhrase = "Кота";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByMinusOfUpdatedPhraseWithUpperCaseInNormalForm() {
        String existingPhrase = "кота";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "Кот Купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    // игнорирование концевых точек при расклейке

    @Test
    public void execute_OneOtherExistingPhraseWithEndingPointsIsAppendedByMinusOfUpdatedPhrase() {
        String existingPhrase = "кота.";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил.";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    // остальные кейсы

    @Test
    public void execute_PhrasesAreSentToUngluingInNormalForms() {
        String existingPhrase = "на кота";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот не купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByFixedMinusOfUpdatedPhrase() {
        String existingPhrase = "123";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "123 на";
        String newPhraseResult = "123 +на";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isUpdatedWithFixation(keywordIdToUpdate, newPhraseResult, newPhrase, newPhraseResult)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "!на"));
    }

    @Test
    public void execute_OneOtherExistingPhraseWithMinusIsAppendedByMinusOfUpdatedPhrase() {
        String existingPhrase = "кот -манул";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    @Test
    public void execute_OneOtherSuspendedExistingPhraseWithMinusIsAppendedByMinusOfUpdatedPhrase() {
        String existingPhrase = "кот -манул";
        createOneActiveAdGroup();
        Long existingKeywordId =
                keywordSteps.createKeyword(adGroupInfo1, getDefaultActiveKeyword(existingPhrase).withIsSuspended(true))
                        .getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить", true));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByTwoMinusesOfUpdatedPhrases() {
        String existingPhrase = "кот";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот купил";
        String newPhrase2 = "кот манул";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, newPhrase1),
                isUpdated(keywordIdToUpdate2, newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase,
                        asList("купить", "манул"), false));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByMinusOfUpdatedDuplicatedPhrases() {
        String existingPhrase = "кот";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот купил";
        String newPhrase2 = "купила кота";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, newPhrase1),
                isNotUpdated(keywordIdToUpdate1, newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "купить"));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsNotAppendedByMinusOfUpdatedPhraseDuplicatedWithOtherExistingPhrase() {
        String existingPhrase1 = "кот";
        String existingPhrase2 = "кот манул -купить";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1).getId();
        Long existingKeywordId2 = createKeyword(adGroupInfo1, existingPhrase2).getId();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот манул -купил";
        String newPhrase2 = "кот манул нормальный тип";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId2, newPhrase1),
                isUpdated(keywordIdToUpdate2, newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    @Test
    public void execute_OneOtherExistingPhraseIsNotAppendedByMinusToAvoidDuplicationWithOtherExistingPhrases() {
        String existingPhrase1 = "кот";
        String existingPhrase2 = "кот -манул";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        createKeyword(adGroupInfo1, existingPhrase2);

        Long existingKeywordId1 = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот манул";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(existingKeywordId1, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(existingKeywordId1, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    @Test
    public void execute_OneOtherExistingPhraseIsNotAppendedByMinusToAvoidDuplicationWithUpdatedPhrases() {
        String existingPhrase1 = "кот";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот манул";
        String newPhrase2 = "кот -манул";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, newPhrase1),
                isUpdated(keywordIdToUpdate2, newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    @Test
    public void execute_OneOtherExistingPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithOtherExistingPhrases() {
        String existingPhrase1 = "кот";
        String existingPhrase2 = "кот -манул -дикий";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo1, existingPhrase2);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String newPhrase1 = "кот манул";
        String newPhrase2 = "дикий кот";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, newPhrase1),
                isUpdated(keywordIdToUpdate2, newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, "манул"));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithUpdatedPhrases() {
        String existingPhrase = "кот";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();
        String newPhrase1 = "кот манул";
        String newPhrase2 = "дикий кот";
        String newPhrase3 = "кот -манул -дикий";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, newPhrase2),
                keywordModelChanges(keywordIdToUpdate3, newPhrase3));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isUpdated(keywordIdToUpdate1, newPhrase1),
                        isUpdated(keywordIdToUpdate2, newPhrase2),
                        isUpdated(keywordIdToUpdate3, newPhrase3)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "манул"));
    }

    @Test
    public void execute_OtherExistingPhraseAndUpdatedPhraseNotSuitableForUngluing() {
        String existingPhrase = "дикий кот";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "домашний пес";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    @Test
    public void execute_OtherExistingPhraseAndUpdatedPhraseAreSuitableForUngluingButInDifferentAdGroups() {
        String existingPhrase = "кот";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo2, existingPhrase);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "домашний кот";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByMinusAndOtherIsNotAppendedBy() {
        String existingPhrase1 = "кота";
        String existingPhrase2 = "домашний пес";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo1, existingPhrase2);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, "купить"));
    }

    @Test
    public void execute_OneOtherExistingPhraseIsAppendedByMinusAndOtherIsNotAppendedByInDifferentAdGroup() {
        String existingPhrase1 = "кота";
        String existingPhrase2 = "домашний пес";
        createTwoActiveAdGroups();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo2, existingPhrase2);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "кот купил";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, "купить"));
    }

    //фраза, которая не может быть распарсена, не участвует в расклейке

    @Test
    public void execute_NotParsedPhraseIsNotSentToUngluing() {
        String existingPhrase = "кот --купить";
        createTwoActiveAdGroups();
        keywordSteps.createKeywordWithText(existingPhrase, adGroupInfo1);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = "домашний кот";
        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, newPhrase);

        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, singletonList(changesKeyword));
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   String addedMinus) {
        return onePhraseIsAffected(id, adGroupId, sourcePhrase, singletonList(addedMinus), false);
    }

    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   String addedMinus, Boolean isSuspended) {
        return onePhraseIsAffected(id, adGroupId, sourcePhrase, singletonList(addedMinus), isSuspended);
    }

    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   List<String> addedMinuses, Boolean isSuspended) {
        return beanDiffer(
                singletonList(new AffectedKeywordInfo(id, adGroupId, sourcePhrase, addedMinuses, isSuspended)));
    }
}
