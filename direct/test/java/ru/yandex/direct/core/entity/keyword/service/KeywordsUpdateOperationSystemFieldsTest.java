package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATE_EVERY_KEYWORD_CHANGE;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationSystemFieldsTest extends KeywordsUpdateOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY =
            onlyFields(newPath(Keyword.ID.name()),
                    newPath(Keyword.STATUS_MODERATE.name()), newPath(Keyword.STATUS_BS_SYNCED.name()),
                    newPath(Keyword.PHRASE_BS_ID.name()), newPath(Keyword.WORDS_COUNT.name()),
                    newPath(Keyword.PHRASE_ID_HISTORY.name()), newPath(Keyword.MODIFICATION_TIME.name()),
                    newPath(Keyword.NEED_CHECK_PLACE_MODIFIED.name()))
                    .forFields(newPath(Keyword.MODIFICATION_TIME.name())).useMatcher(approximatelyNow());

    @Test
    public void noChanges() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        assertActiveKeywordSystemFieldsIsNotChanged(keywordIdToUpdate, 1, false);
    }

    @Test
    public void systemFieldsAreNotChangedWhenPhraseIsChangedButNormPhraseIsNotChanged() {
        String phrase = "купить слон";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String expectedPhrase = "купить слона";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, expectedPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, expectedPhrase)));
        if (ppcPropertiesSupport.get(MODERATE_EVERY_KEYWORD_CHANGE).get()) {
            assertActiveKeywordStatusModerateSyncedChanged(keywordIdToUpdate, 2, true);
        } else {
            assertActiveKeywordSystemFieldsIsNotChanged(keywordIdToUpdate, 2, true);
        }
    }

    @Test
    public void systemFieldsAreNotChangedWhenMinusWordIsAddedToPhrase() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String newPhrase = PHRASE_1 + " -новоеслово";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertActiveKeywordSystemFieldsIsNotChanged(keywordIdToUpdate, 1, true);
    }

    @Test
    public void systemFieldsAreNotChangedWhenMinusWordIsRemovedFromPhrase() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1 + " -новоеслово").getId();

        String newPhrase = PHRASE_1;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        assertActiveKeywordSystemFieldsIsNotChanged(keywordIdToUpdate, 1, true);
    }

    @Test
    public void statusModerateIsDroppedWhenWordIsAddedToNormPhrase() {
        String phrase = "купить слон";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String expectedPhrase = "купить слона серьезного";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, expectedPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, expectedPhrase)));

        Keyword expectedKeyword = getExpectedKeyword(keywordIdToUpdate, 3, true)
                .withPhrase(expectedPhrase)
                .withStatusModerate(StatusModerate.NEW);
        assertKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void statusModerateIsDroppedWhenWordIsChangedInNormPhrase() {
        String phrase = "купить слон";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String expectedPhrase = "купить коня";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, expectedPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, expectedPhrase)));

        Keyword expectedKeyword = getExpectedKeyword(keywordIdToUpdate, 2, true)
                .withPhrase(expectedPhrase)
                .withPhraseBsId(BigInteger.ZERO)
                .withPhraseIdHistory(null)
                .withStatusModerate(StatusModerate.NEW);
        assertKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // поведение при обновлении невалидных фраз

    // если исправляется невалидная фраза, то статус модерации и т.п. сбрасываются
    @Test
    public void invalidPhraseFixedAndNormPhraseIsNotChanged() {
        String oldInvalidPhrase = "купить сло!н";
        String oldNormPhrase = "купить слон";
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword("купить слон")
                .withPhrase(oldInvalidPhrase)
                .withNormPhrase(oldNormPhrase);
        Long keywordIdToUpdate = keywordSteps.createKeyword(adGroupInfo1, keyword).getId();

        String newPhrase = "купить слона";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        Keyword expectedKeyword = getExpectedKeyword(keywordIdToUpdate, 2, true)
                .withStatusModerate(StatusModerate.NEW)
                .withPhraseBsId(BigInteger.ZERO)
                .withPhraseIdHistory(null);
        assertKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // если исправляется невалидная фраза, то статус модерации и т.п. сбрасываются
    @Test
    public void invalidPhraseFixedAndNormPhraseIsChanged() {
        String oldInvalidPhrase = "купить сло!н дешево";
        String oldNormPhrase = "дешево купить слон";
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword("купить слон дешево")
                .withPhrase(oldInvalidPhrase)
                .withNormPhrase(oldNormPhrase);
        Long keywordIdToUpdate = keywordSteps.createKeyword(adGroupInfo1, keyword).getId();

        String newPhrase = "купить слона";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        Keyword expectedKeyword = getExpectedKeyword(keywordIdToUpdate, 2, true)
                .withStatusModerate(StatusModerate.NEW)
                .withPhraseBsId(BigInteger.ZERO)
                .withPhraseIdHistory(null);
        assertKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // если исправляется только невалидное поле norm_phrase без изменения текста фразы,
    // то статус модерации и т.п. не сбрасываются
    @Test
    public void validPhraseIsNotChangedAndInvalidNormPhraseFixed() {
        String oldPhrase = "купить слон -дешево";
        String oldInvalidNormPhrase = "-дешево купить слон";
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword(oldPhrase)
                .withNormPhrase(oldInvalidNormPhrase);
        Long keywordIdToUpdate = keywordSteps.createKeyword(adGroupInfo1, keyword).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, oldPhrase, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, oldPhrase)));
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate, 2, true);
    }

    // здесь проверяем, что при изменении валидной фразы, у которой
    // нормальная форма была невалидна, ничего не падает
    @Test
    public void validPhraseIsChangedAndInvalidNormPhraseFixed() {
        String oldPhrase = "купить слон -дешево";
        String oldInvalidNormPhrase = "-дешево купить слон";
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword(oldPhrase)
                .withNormPhrase(oldInvalidNormPhrase);
        Long keywordIdToUpdate = keywordSteps.createKeyword(adGroupInfo1, keyword).getId();

        String newPhrase = "купить собаку";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));
        Keyword expectedKeyword = getExpectedKeyword(keywordIdToUpdate, 2, true)
                .withStatusModerate(StatusModerate.NEW)
                .withPhraseBsId(BigInteger.ZERO)
                .withPhraseIdHistory(null);
        assertKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void statusBsSyncedIsDroppedWhenPhriceIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate, 1, true);
    }

    @Test
    public void statusBsSyncedIsDroppedWhenPriceContextIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Long newPriceContext = 10L;
        List<ModelChanges<Keyword>> changesKeywords = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                        .process(BigDecimal.valueOf(newPriceContext), Keyword.PRICE_CONTEXT));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate, 1, false);
    }

    @Test
    public void statusBsSyncedIsDroppedWhenPriorityIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Integer newAutoBudgetPriority = 5;
        List<ModelChanges<Keyword>> changesKeywords = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                        .process(newAutoBudgetPriority, Keyword.AUTOBUDGET_PRIORITY));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate, 1, false);
    }

    @Test
    public void statusBsSyncedIsDroppedForTwoKeywordsWhenPriceIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_1, 10L),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_2, 11L));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_1),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate1, 1, true);
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate2, 2, true);
    }

    @Test
    public void existingDuplicatedKeywordStatusBsSyncedIsNotChanged() {
        createTwoActiveAdGroups();

        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_1, 10L),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_2, 11L));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, PHRASE_1),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertActiveKeywordSystemFieldsIsNotChanged(existingKeywordId, 1, false);
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate2, 2, true);
    }

    @Test
    public void statusBsSyncedIsDroppedForValidKeywordWhenInvalidKeywordPresents() {
        createTwoActiveAdGroups();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, INVALID_PHRASE_1, 10L),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_2, 11L));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(null, isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertActiveKeywordSystemFieldsIsNotChanged(keywordIdToUpdate1, 1, false);
        assertActiveKeywordStatusBsSyncedChanged(keywordIdToUpdate2, 2, true);
    }

    // ассерты

    private void assertActiveKeywordStatusBsSyncedChanged(Long keywordId, int wordsCount, boolean needCheckPlace) {
        Keyword expectedKeyword = getExpectedKeyword(keywordId, wordsCount, needCheckPlace)
                .withStatusBsSynced(StatusBsSynced.NO);
        assertKeyword(keywordId, expectedKeyword);
    }

    private void assertActiveKeywordSystemFieldsIsNotChanged(Long keywordId, int wordsCount, boolean needCheckPlace) {
        Keyword expectedKeyword = getExpectedKeyword(keywordId, wordsCount, needCheckPlace);
        assertKeyword(keywordId, expectedKeyword);
    }

    private void assertActiveKeywordStatusModerateSyncedChanged(Long keywordId, int wordsCount,
                                                                boolean needCheckPlace) {
        Keyword expectedKeyword = getExpectedKeyword(keywordId, wordsCount, needCheckPlace)
                .withStatusModerate(StatusModerate.NEW);
        assertKeyword(keywordId, expectedKeyword);
    }

    private Keyword getExpectedKeyword(Long keywordId, int wordsCount, boolean needCheckPlace) {
        return new Keyword()
                .withId(keywordId)
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withPhraseBsId(BigInteger.ONE)
                .withWordsCount(wordsCount)
                .withPhraseIdHistory(History.parse("O1"))
                .withNeedCheckPlaceModified(needCheckPlace);
    }

    private void assertKeyword(Long keywordId, Keyword expectedKeyword) {
        Keyword actualKeyword = keywordRepository
                .getKeywordsByIds(clientInfo.getShard(), clientInfo.getClientId(), singletonList(keywordId)).get(0);
        assertThat("состояние ключевой фразы не соответствует ожидаемому",
                actualKeyword, beanDiffer(expectedKeyword).useCompareStrategy(COMPARE_STRATEGY));
    }
}
