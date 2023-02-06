package ru.yandex.direct.core.entity.keyword.service;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdatePhraseModificationTest extends KeywordsUpdateOperationBaseTest {

    /**
     * Suspend двух кейвордов с одинаковыми фразами
     */
    @Test
    public void execute_WithoutPhraseModification_SameWithNew_Test() {
        createOneActiveAdGroup();
        Long firstKeywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        Long secondKeywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = asList(
                new ModelChanges<>(firstKeywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED),
                new ModelChanges<>(secondKeywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartialWithoutPhraseModification(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(2));
        List<Long> resultIds = mapList(result.getResult(), t -> t.getResult().getId());
        assertThat("Кейворды не должны склеиться", resultIds.stream().distinct().count(), is(2L));
    }

    /**
     * Suspend одного кейворда с фразой такой же, как у существующего suspended
     */
    @Test
    public void execute_WithoutPhraseModification_SameWithExistingSuspended_Test() {
        createOneActiveAdGroup();
        KeywordInfo existingKeyword = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1);
        steps.keywordSteps().updateKeywordProperty(existingKeyword, Keyword.IS_SUSPENDED, true);
        Long keywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartialWithoutPhraseModification(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(1));
        assertThat("Кейворды не должны склеиться", result.getResult().get(0).getResult().getId(), is(keywordIdToUpdate));
    }

    /**
     * Suspend одного кейворда с фразой такой же, как у существующего активного
     */
    @Test
    public void execute_WithoutPhraseModification_SameWithExistingActive_Test() {
        createOneActiveAdGroup();
        KeywordInfo existingKeyword = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1);
        Long keywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartialWithoutPhraseModification(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(1));
        assertThat("Кейворды не должны склеиться", result.getResult().get(0).getResult().getId(), is(keywordIdToUpdate));
    }

    /**
     * Обычный update двух кейвордов с одинаковыми фразами
     */
    @Test
    public void execute_WithPhraseModification_SameWithNew_Test() {
        createOneActiveAdGroup();
        Long firstKeywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        Long secondKeywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = asList(
                new ModelChanges<>(firstKeywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED),
                new ModelChanges<>(secondKeywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(2));
        List<Long> resultIds = mapList(result.getResult(), t -> t.getResult().getId());
        assertThat("Кейворды должны склеиться", resultIds.stream().distinct().count(), is(1L));
    }

    /**
     * Обычный update одного кейворда с фразой такой же, как у существующего suspended
     */
    @Test
    public void execute_WithPhraseModification_SameWithExistingSuspended_Test() {
        createOneActiveAdGroup();
        KeywordInfo existingKeyword = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1);
        steps.keywordSteps().updateKeywordProperty(existingKeyword, Keyword.IS_SUSPENDED, true);
        Long keywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(1));
        assertThat("Кейворды должны склеиться", result.getResult().get(0).getResult().getId(), is(existingKeyword.getId()));
    }

    /**
     * Обычный update одного кейворда с фразой такой же, как у существующего активного
     */
    @Test
    public void execute_WithPhraseModification_SameWithExistingActive_Test() {
        createOneActiveAdGroup();
        Long existingKeywordId = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        Long keywordIdToUpdate = steps.keywordSteps().createKeywordWithText(PHRASE_2, adGroupInfo1).getId();
        List<ModelChanges<Keyword>> keywordModelChanges = singletonList(
                new ModelChanges<>(keywordIdToUpdate, Keyword.class).process(true, Keyword.IS_SUSPENDED));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywordModelChanges);
        assertThat("Обновление должно пройти успешно", result.getValidationResult().hasAnyErrors(), is(false));
        assertThat("Должны вернуть результат для каждого кейворда из запроса", result.getResult().size(), is(1));
        assertThat("Кейворды должны склеиться", result.getResult().get(0).getResult().getId(), is(existingKeywordId));
    }
}
