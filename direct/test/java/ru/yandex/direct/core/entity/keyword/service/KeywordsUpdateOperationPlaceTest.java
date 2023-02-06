package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationPlaceTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void execute_NoChanges_PlaceNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Place expectedPlace = getKeyword(keywordIdToUpdate).getPlace();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertThat(getKeyword(keywordIdToUpdate).getPlace(), is(expectedPlace));
    }

    @Test
    public void execute_ChangePrice_PlaceChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Place oldPlace = getKeyword(keywordIdToUpdate).getPlace();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        assertThat(getKeyword(keywordIdToUpdate).getPlace(), not(is(oldPlace)));
    }

    @Test
    public void execute_NormPhraseNoChange_BsAuctionCalled() {
        String phrase = "купить слон";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String expectedPhrase = "купить слона";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, expectedPhrase));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, expectedPhrase)));
        verify(keywordBsAuctionService).getTrafaretAuction(any(ClientId.class), any(), any());
    }

    @Test
    public void execute_ChangePrice_BsAuctionCalled() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        verify(keywordBsAuctionService).getTrafaretAuction(any(ClientId.class), any(), any());
    }

    @Test
    public void execute_TwoAdGroups_BsAuctionCalled() {
        createTwoActiveAdGroups();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        verify(keywordBsAuctionService).getTrafaretAuction(any(ClientId.class), any(), any());
    }

    @Test
    public void execute_KeywordIsInvalid_BsAuctionCalled() {
        createOneActiveAdGroup();

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        MassResult<UpdatedKeywordInfo> result =
                executePartial(asList(keywordModelChanges(keywordIdToUpdate1, INVALID_PHRASE_1),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_3)));
        assumeThat(result, isSuccessfulWithMatchers(null, isUpdated(keywordIdToUpdate2, PHRASE_3)));

        verify(keywordBsAuctionService).getTrafaretAuction(any(ClientId.class), any(), any());
    }

    @Test
    public void execute_KeywordIsDuplicated_PlaceNotChanged() {
        createOneActiveAdGroup();

        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Place expectedPlace = getKeyword(keywordIdToUpdate).getPlace();

        MassResult<UpdatedKeywordInfo> result =
                executePartial(singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1)));
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, PHRASE_1)));

        // т.к. ключевик задублировался с существующим, получаем из БД по старому id
        assertThat(getKeyword(existingKeywordId).getPlace(), is(expectedPlace));
    }
}
