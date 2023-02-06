package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.addedInfoMatcher;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixations;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationStopwordsFixationTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_PhraseWithoutFixations_ResultsHasNoFixations() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
    }

    @Test
    public void execute_PhraseWithOnePlusWordsFixation_ResultsHasOneFixation() {
        String phrase = "наша 5";
        String resultPhrase = "+наша 5";
        String resultNormPhrase = "+наша 5";
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(resultPhrase, "наша 5", "+наша 5")));

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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(resultPhrase, "Наша 5", "+Наша 5")));

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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(resultPhrase, "-на", "-!на")));

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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(resultPhrase, "-На", "-!На")));

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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAddedWithFixation(resultPhrase, "-на", "-!на")));

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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(
                isAddedWithFixations(resultPhrase,
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
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(addedInfoMatcher(
                adGroupInfo1.getAdGroupId(), true, resultPhrase, resultPhrase, fixations, null)));

        Keyword keyword = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword.getPhrase(), equalTo(resultPhrase));
        assertThat(keyword.getNormPhrase(), equalTo(resultNormPhrase));
    }

    @Test
    public void execute_TwoItemsWithFixationsInDifferentAdGroupsAndOneWithout_ResultsHasOneFixation() {
        String phrase1 = "зафиксируй меня полностью -на";
        String resultPhrase1 = "зафиксируй меня полностью -!на";
        String resultNormPhrase1 = "зафиксировать полностью";

        String phrase2 = "на 5";
        String resultPhrase2 = "+на 5";
        String resultNormPhrase2 = "+на 5";

        createTwoActiveAdGroups();
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1),
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, phrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assertThat(result, isSuccessfulWithMatchers(
                isAddedWithFixation(resultPhrase1, "-на", "-!на"),
                isAdded(PHRASE_1),
                isAddedWithFixation(resultPhrase2, "на 5", "+на 5")));

        Keyword keyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(keyword1.getPhrase(), equalTo(resultPhrase1));
        assertThat(keyword1.getNormPhrase(), equalTo(resultNormPhrase1));

        Keyword keyword2 = getKeyword(result.get(2).getResult().getId());
        assertThat(keyword2.getPhrase(), equalTo(resultPhrase2));
        assertThat(keyword2.getNormPhrase(), equalTo(resultNormPhrase2));
    }
}
