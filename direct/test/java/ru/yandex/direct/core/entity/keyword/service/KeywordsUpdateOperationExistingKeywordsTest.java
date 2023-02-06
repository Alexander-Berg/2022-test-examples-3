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
public class KeywordsUpdateOperationExistingKeywordsTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_OnePhraseThatIsDuplicateWithExistingPhrase_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Keyword existingKeyword = createKeyword(adGroupInfo1, PHRASE_1).getKeyword();
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, PHRASE_2).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), PHRASE_1));
        MassResult<UpdatedKeywordInfo> result =
                executePartial(changesKeywords, asList(existingKeyword, keywordToUpdate));

        assertThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeyword.getId(), PHRASE_1)));
        checkValidationHasDuplicateInExistingWarnings(result, true);
    }

    @Test
    public void execute_OnePhraseThatIsDuplicateWithExistingPhraseNotInExistingList_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, PHRASE_1);
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, PHRASE_2).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords, singletonList(keywordToUpdate));

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), PHRASE_1)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }

    @Test
    public void execute_OnePhraseWithNotParsedExistingPhrase_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Keyword existingKeyword = keywordSteps.createKeywordWithText(PREINVALID_PHRASE_1, adGroupInfo1).getKeyword()
                .withIsSuspended(false);
        Keyword keywordToUpdate = createKeyword(adGroupInfo1, PHRASE_2).getKeyword();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordToUpdate.getId(), PHRASE_1));
        MassResult<UpdatedKeywordInfo> result =
                executePartial(changesKeywords, asList(existingKeyword, keywordToUpdate));

        assertThat(result, isSuccessfulWithMatchers(isUpdated(keywordToUpdate.getId(), PHRASE_1)));
        checkValidationHasDuplicateInExistingWarnings(result, false);
    }
}
