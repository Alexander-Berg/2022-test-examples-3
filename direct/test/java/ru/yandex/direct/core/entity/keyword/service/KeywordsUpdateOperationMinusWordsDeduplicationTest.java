package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationMinusWordsDeduplicationTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_PhraseWithoutMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase = "фраза и ничего кроме фразы";
        String normPhrase = "кроме ничто фраза";
        List<ModelChanges<Keyword>> changes = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(phrase));
        assertThat(keyword.getNormPhrase(), equalTo(normPhrase));
    }

    @Test
    public void execute_PhraseWithNonDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase = "фраза -ололо";
        String normPhrase = "фраза";
        List<ModelChanges<Keyword>> changes = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(phrase));
        assertThat(keyword.getNormPhrase(), equalTo(normPhrase));
    }

    @Test
    public void execute_TwoPhrasesWithNonDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "фраза и ничего -кроме";
        String normPhrase1 = "ничто фраза";
        String phrase2 = "другая -фраза";
        String normPhrase2 = "другой";
        List<ModelChanges<Keyword>> changes = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, phrase1),
                isUpdated(keywordIdToUpdate2, phrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(phrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(normPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(phrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(normPhrase2));
    }

    @Test
    public void execute_PhraseWithDuplicatedMinusWordsAndOtherWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase = "слон розовый -пес -конь -сорбокотонеастер -коню -кот";
        String resultPhrase = "слон розовый -пес -конь -сорбокотонеастер -кот";
        String resultNormPhrase = "розовый слон";
        List<ModelChanges<Keyword>> changes = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, resultPhrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_OneOfPhrasesWithDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "слон розовый -пес -конь -сорбокотонеастер -коню -кот";
        String resultPhrase1 = "слон розовый -пес -конь -сорбокотонеастер -кот";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон конь -кот -пес";
        String normPhrase2 = "конь слон";
        List<ModelChanges<Keyword>> changes = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, resultPhrase1),
                isUpdated(keywordIdToUpdate2, phrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(phrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(normPhrase2));
    }

    /**
     * Существующая фраза имеет дублированные минус-слова,
     * а мы меняем только ставку
     */
    @Test
    public void execute_ExistingAndNotChangesPhraseWithDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        String existingPhrase = "слон розовый -коню -пес -конь";
        String resultExistingPhrase = "слон розовый -коню -пес";
        String resultNormalExistingPhrase = "розовый слон";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, existingPhrase).getId();

        ModelChanges<Keyword> modelChanges = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(123d), Keyword.PRICE);
        List<ModelChanges<Keyword>> changes = singletonList(modelChanges);

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, resultExistingPhrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultExistingPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormalExistingPhrase));
    }

    @Test
    public void execute_OnePhraseWithDuplicatedMultiLemmasMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -уху";
        String resultPhrase1 = "слон розовый -уху";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -уху -ухо -конь";
        String resultPhrase2 = "слон розовый -уху -конь";
        String resultNormPhrase2 = "розовый слон";
        List<ModelChanges<Keyword>> changes = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, resultPhrase1),
                isUpdated(keywordIdToUpdate2, resultPhrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(resultPhrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(resultNormPhrase2));
    }


    @Test
    public void execute_OnePhraseWithDuplicatedFixedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -!уху";
        String resultPhrase1 = "слон розовый -ухо";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -!уху -ухо -конь";
        String resultPhrase2 = "слон розовый -ухо -конь";
        String resultNormPhrase2 = "розовый слон";
        List<ModelChanges<Keyword>> changes = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, resultPhrase1),
                isUpdated(keywordIdToUpdate2, resultPhrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(resultPhrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(resultNormPhrase2));
    }

    @Test
    public void execute_OnePhraseWithDuplicatedFixedWithPlusMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -+уху";
        String resultPhrase1 = "слон розовый -+уху";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -+уху -ухо -конь";
        String resultPhrase2 = "слон розовый -+уху -конь";
        String resultNormPhrase2 = "розовый слон";
        List<ModelChanges<Keyword>> changes = asList(
                keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changes);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, resultPhrase1),
                isUpdated(keywordIdToUpdate2, resultPhrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(resultPhrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(resultNormPhrase2));
    }
}
