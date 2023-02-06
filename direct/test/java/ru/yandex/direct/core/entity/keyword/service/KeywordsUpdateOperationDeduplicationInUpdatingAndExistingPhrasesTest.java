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
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdatedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * Тесты обработки обновляемых фраз, которые дублируются одновременно
 * между собой и с существующими фразами.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationDeduplicationInUpdatingAndExistingPhrasesTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_UpdateTwoPhrasesDuplicatedWithOtherExistingAndByEachOther_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_1),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, PHRASE_1),
                isNotUpdated(existingKeywordId, PHRASE_1)));
        checkKeywordsDeleted(keywordIdToUpdate1, keywordIdToUpdate2);
        checkValidationHasDuplicateInExistingWarnings(result, true, true);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false);
    }

    @Test
    public void execute_OneUniquePhraseAndTwoUpdatedPhrasesDuplicatedWithOtherExistingAndByEachOther_ResultHasCorrectInfo() {
        String existingPhrase = "дедупликация работает -!не";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_3).getId();
        Long keywordIdToUpdate3 = createKeyword(adGroupInfo1, PHRASE_3).getId();

        String newPhrase1 = "работает дедупликация -не";
        String newPhrase1Exp = "работает дедупликация -!не";
        String newPhrase2 = "работает дедупликация работает  -!не";
        String newPhrase2Exp = "работает дедупликация работает -!не";
        String uniquePhrase = "у нас все работает как часы";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, newPhrase1),
                keywordModelChanges(keywordIdToUpdate2, uniquePhrase),
                keywordModelChanges(keywordIdToUpdate3, newPhrase2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);

        assertThat(result, isSuccessfulWithMatchers(
                isNotUpdatedWithFixation(existingKeywordId, newPhrase1Exp, "-не", "-!не"),
                isUpdated(keywordIdToUpdate2, uniquePhrase),
                isNotUpdated(existingKeywordId, newPhrase2Exp)));
        checkKeywordsDeleted(keywordIdToUpdate1, keywordIdToUpdate3);
        checkValidationHasDuplicateInExistingWarnings(result, true, false, true);
        checkValidationHasDuplicateInUpdatedWarnings(result, false, false, false);
    }
}
