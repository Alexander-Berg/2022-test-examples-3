package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds.Gen.PLUS_MARK_IN_BRACKETS;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationUnfixStopwordsInQuotesTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_StopwordWithDuplicatesInQuotes() {
        createOneActiveAdGroup();

        String actualPhrase = "\"слово +за слово\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        String expectedPhrase = "\"слово за слово\"";
        assertThat(result, isSuccessfulWithMatchers(isAdded(expectedPhrase)));
    }

    @Test
    public void execute_StopwordsWithHyphenInQuotes() {
        createOneActiveAdGroup();

        String actualPhrase = "\"+по-русски\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        String expectedPhrase = "\"по-русски\"";
        assertThat(result, isSuccessfulWithMatchers(isAdded(expectedPhrase)));
    }

    @Test
    public void execute_PhraseWithStopwordsInQuotes() {
        createOneActiveAdGroup();

        String actualPhrase = "\"+на конь\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        String expectedPhrase = "\"на конь\"";
        assertThat(result, isSuccessfulWithMatchers(isAdded(expectedPhrase)));
    }

    @Test
    public void execute_PhraseInQuotesWithSomePlusesBeforeWords() {
        createOneActiveAdGroup();

        String actualPhrase = "\"+не +вакуумному !коню\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        String expectedPhrase = "\"не вакуумному !коню\"";
        assertThat(result, isSuccessfulWithMatchers(isAdded(expectedPhrase)));
    }

    @Test
    public void execute_PhraseInQuotesWithVariousSpecialCharacters() {
        createOneActiveAdGroup();

        String actualPhrase = "\"[вакуумному !коню] +в зубы не !смотрят\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        String expectedPhrase = "\"[вакуумному !коню] в зубы не !смотрят\"";
        assertThat(result, isSuccessfulWithMatchers(isAdded(expectedPhrase)));
    }

    @Test
    public void execute_StopwordsInPhraseInSquareQuotes() {
        createOneActiveAdGroup();

        String actualPhrase = "\"[слово +за слово]\"";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, actualPhrase));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("phrase")),
                        PLUS_MARK_IN_BRACKETS)));
    }
}
