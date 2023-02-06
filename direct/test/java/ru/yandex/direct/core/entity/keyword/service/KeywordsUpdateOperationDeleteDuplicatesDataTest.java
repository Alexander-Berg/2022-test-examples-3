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
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationDeleteDuplicatesDataTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_OneOfUpdatedPhrasesIsDuplicatedWithExisting_PhraseIsDeletedFromAllTables() {
        String existingPhrase = "персик";
        createOneActiveAdGroup();
        KeywordInfo existingKeywordInfo =
                createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo1, existingPhrase);
        KeywordInfo updatedKeywordInfo = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo1, PHRASE_1);

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(updatedKeywordInfo.getId(), existingPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordInfo.getId(), existingPhrase)));

        assertThat(isBidsBaseRecordNotPresent(updatedKeywordInfo), is(true));
        assertThat(isBidsRecordPresent(updatedKeywordInfo), is(false));
        assertThat(isBidsHrefParamsRecordPresent(updatedKeywordInfo), is(false));
        assertThat(isBidsPhraseIdHistoryRecordPresent(updatedKeywordInfo), is(false));
        assertThat(isBidsManualPricesRecordPresent(updatedKeywordInfo), is(false));
    }

    @Test
    public void execute_OneOfUpdatedPhrasesIsDuplicatedWithOtherUpdated_PhraseIsDeletedFromAllTables() {
        createOneActiveAdGroup();
        KeywordInfo updatedKeywordInfo1 = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo1, PHRASE_1);
        KeywordInfo updatedKeywordInfo2 = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo1, PHRASE_2);

        String newPhrase = "персик";
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(updatedKeywordInfo1.getId(), newPhrase),
                keywordModelChanges(updatedKeywordInfo2.getId(), newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(updatedKeywordInfo1.getId(), newPhrase),
                isNotUpdated(updatedKeywordInfo1.getId(), newPhrase)));

        assertThat(isBidsBaseRecordNotPresent(updatedKeywordInfo2), is(true));
        assertThat(isBidsRecordPresent(updatedKeywordInfo2), is(false));
        assertThat(isBidsHrefParamsRecordPresent(updatedKeywordInfo2), is(false));
        assertThat(isBidsPhraseIdHistoryRecordPresent(updatedKeywordInfo2), is(false));
        assertThat(isBidsManualPricesRecordPresent(updatedKeywordInfo2), is(false));
    }
}
