package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationMinusWordsDeduplicationTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_PhraseWithoutMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase = "фраза и ничего кроме фразы";
        String normPhrase = "кроме ничто фраза";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(phrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(phrase));
        assertThat(keyword.getNormPhrase(), equalTo(normPhrase));
    }

    @Test
    public void execute_OnePhraseWithNonDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase = "фраза -ололо";
        String normPhrase = "фраза";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(phrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(phrase));
        assertThat(keyword.getNormPhrase(), equalTo(normPhrase));
    }

    @Test
    public void execute_TwoPhrasesWithNonDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "фраза и ничего -кроме";
        String normPhrase1 = "ничто фраза";
        String phrase2 = "другая -фраза";
        String normPhrase2 = "другой";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(phrase1), isAdded(phrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(phrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(normPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(phrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(normPhrase2));
    }

    @Test
    public void execute_OnePhraseWithDuplicatedMinusWordsAndOtherWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase = "слон розовый -пес -конь -сорбокотонеастер -коню -кот";
        String resultPhrase = "слон розовый -пес -конь -сорбокотонеастер -кот";
        String resultNormPhrase = "розовый слон";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(resultPhrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_OneOfPhrasesWithDuplicatedMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "слон розовый -пес -конь -сорбокотонеастер -коню -кот";
        String resultPhrase1 = "слон розовый -пес -конь -сорбокотонеастер -кот";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон конь -кот -пес";
        String normPhrase2 = "конь слон";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(resultPhrase1), isAdded(phrase2)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(phrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(normPhrase2));
    }

    @Test
    public void execute_OnePhraseWithDuplicatedMultiLemmasMinusWords_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -уху";
        String resultPhrase1 = "слон розовый -уху";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -уху -ухо -конь";
        String resultPhrase2 = "слон розовый -уху -конь";
        String resultNormPhrase2 = "розовый слон";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(resultPhrase1), isAdded(resultPhrase2)));

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

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -!уху";
        String resultPhrase1 = "слон розовый -ухо";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -!уху -ухо -конь";
        String resultPhrase2 = "слон розовый -ухо -конь";
        String resultNormPhrase2 = "розовый слон";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(resultPhrase1), isAdded(resultPhrase2)));

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

        // проверяем, что правильное слово удаляется независимо от порядка следования
        String phrase1 = "слон розовый -ухо -+уху";
        String resultPhrase1 = "слон розовый -+уху";
        String resultNormPhrase1 = "розовый слон";
        String phrase2 = "слон розовый -+уху -ухо -конь";
        String resultPhrase2 = "слон розовый -+уху -конь";
        String resultNormPhrase2 = "розовый слон";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, phrase2));

        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(resultPhrase1), isAdded(resultPhrase2)));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(resultPhrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(resultNormPhrase2));
    }
}
