package ru.yandex.direct.core.entity.keyword.service;

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
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationUnfixStopwordsInQuotesTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_OnePhraseWithStopWordInQuotes() {
        createOneActiveAdGroup();

        String phraseWithStopWord = "\"слово +за слово\"";
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, phraseWithStopWord).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), phraseWithStopWord));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        String phraseWithStopWordAfterUpdate = "\"слово за слово\"";
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), phraseWithStopWordAfterUpdate)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_StopWordsWithHyphenInQuotes() {
        createOneActiveAdGroup();

        String phraseWithStopWord = "\"+по-русски\"";
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, phraseWithStopWord).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), phraseWithStopWord));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        String phraseWithStopwordAfterUpdate = "\"по-русски\"";
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), phraseWithStopwordAfterUpdate)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_PhraseWithStopwordsInQuotes() {
        createOneActiveAdGroup();

        String phraseWithStopWord = "\"+на конь\"";
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, phraseWithStopWord).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), phraseWithStopWord));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        String phraseWithStopwordAfterUpdate = "\"на конь\"";
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), phraseWithStopwordAfterUpdate)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_PhraseInQuotesWithSomePlusesBeforeWords() {
        createOneActiveAdGroup();

        String phraseWithStopWord = "\"+не +вакуумному !коню\"";
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, phraseWithStopWord).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), phraseWithStopWord));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        String phraseWithStopwordAfterUpdate = "\"не вакуумному !коню\"";
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), phraseWithStopwordAfterUpdate)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_PhraseInQuotesWithVariousSpecialCharacters() {
        createOneActiveAdGroup();

        String phraseWithStopWord = "\"[вакуумному !коню] +в зубы не !смотрят\"";
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, phraseWithStopWord).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), phraseWithStopWord));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        String phraseWithStopwordAfterUpdate = "\"[вакуумному !коню] в зубы не !смотрят\"";
        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), phraseWithStopwordAfterUpdate)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_StopwordWithDuplicatesInQuotes() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String phrase1 = "\"слово за слово\"";
        String phrase2 = "\"слово +за слово\"";
        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, phrase1),
                keywordModelChanges(keywordIdToUpdate2, phrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, phrase1),
                isNotUpdated(keywordIdToUpdate1, phrase1)));
        checkKeywordsDeleted(keywordIdToUpdate2);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, true);
    }

}
