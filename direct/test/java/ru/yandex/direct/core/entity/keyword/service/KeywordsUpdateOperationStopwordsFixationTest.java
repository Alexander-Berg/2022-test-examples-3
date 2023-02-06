package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;
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
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdatedWithFixations;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.updatedInfoMatcher;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationStopwordsFixationTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_PhraseWithoutFixations_ResultsHasNoFixations() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
    }

    @Test
    public void execute_PhraseWithOnePlusWordsFixation_ResultsHasOneFixation() {
        String phrase = "наша 5";
        String resultPhrase = "+наша 5";
        String resultNormPhrase = "+наша 5";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, resultPhrase, phrase, resultPhrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_PhraseWithOnePlusWordsFixationInUpperCase_ResultsHasOneFixation() {
        String phrase = "Наша 5";
        String resultPhrase = "+Наша 5";
        String resultNormPhrase = "+наша 5";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, resultPhrase, phrase, resultPhrase)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_PhraseWithOneMinusWordsFixation_ResultsHasOneFixation() {
        String phrase = "зафиксируй меня полностью -на";
        String resultPhrase = "зафиксируй меня полностью -!на";
        String resultNormPhrase = "зафиксировать полностью";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, resultPhrase, "-на", "-!на")));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_PhraseWithOneMinusWordsFixationInUpperCase_ResultsHasOneFixation() {
        String phrase = "зафиксируй меня полностью -На";
        String resultPhrase = "зафиксируй меня полностью -!На";
        String resultNormPhrase = "зафиксировать полностью";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, resultPhrase, "-На", "-!На")));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_FixationWorksFineWithDuplicatedStopWords() {
        String phrase = "зафиксируй меня полностью -на -на";
        String resultPhrase = "зафиксируй меня полностью -!на";
        String resultNormPhrase = "зафиксировать полностью";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate, resultPhrase, "-на", "-!на")));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_FixationWorksFineWithDuplicatedAndUpperCaseStopWordsAndOtherWords() {
        String phrase = "зафиксируй меня полностью -на -слон -не -На -конь -не";
        String resultPhrase = "зафиксируй меня полностью -!на -слон -!не -конь";
        String resultNormPhrase = "зафиксировать полностью";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result, isSuccessfulWithMatchers(
                isUpdatedWithFixations(keywordIdToUpdate, resultPhrase,
                        asList(new StopwordsFixation("-на", "-!на"), new StopwordsFixation("-не", "-!не")))));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_PhraseWithOnePlusWordFixationAndTwoMinusWordsFixation_ResultsHasAllFixation() {
        String phrase = "не 5 -на -!под -наша";
        String resultPhrase = "+не 5 -!на -!под -!наша";
        String resultNormPhrase = "+не 5";

        List<StopwordsFixation> fixations = asList(
                new StopwordsFixation("не 5", "+не 5"),
                new StopwordsFixation("-на", "-!на"),
                new StopwordsFixation("-наша", "-!наша"));

        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, phrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result, isSuccessfulWithMatchers(
                updatedInfoMatcher(keywordIdToUpdate, resultPhrase, resultPhrase, false, fixations, null, false)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_OneItemWithFixationAndOneWithout_ResultsHasOneFixation() {
        String phrase = "зафиксируй меня полностью -на";
        String resultPhrase = "зафиксируй меня полностью -!на";
        String resultNormPhrase = "зафиксировать полностью";
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, phrase),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assertThat(result,
                isSuccessfulWithMatchers(isUpdatedWithFixation(keywordIdToUpdate1, resultPhrase, "-на", "-!на"),
                        isUpdated(keywordIdToUpdate2, PHRASE_3)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }
}
