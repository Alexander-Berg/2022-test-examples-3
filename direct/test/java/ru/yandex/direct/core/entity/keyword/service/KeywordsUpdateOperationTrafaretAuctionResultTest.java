package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationTrafaretAuctionResultTest extends KeywordsUpdateOperationBaseTest {


    @Test
    public void getTrafaretBidsByIndexMap_AddNewKeyword_TrafaretBidsContainsResult() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));
        ImmutableMap<Integer, List<TrafaretBidItem>> trafaretBidsByIndexMap = operation.getTrafaretBidsByIndexMap();

        assertThat(trafaretBidsByIndexMap,
                beanDiffer(singletonMap(0, defaultBsTrafaretAuctionData(null).getBidItems())));
    }

    @Test
    public void getTrafaretBidsByIndexMap_AddKeywordWithDuplicate_ReturnsTrafaretBidsOnlyByUniqueKeyword() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_3),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        assumeThat(operation.prepareAndApply(), isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_3),
                isNotUpdated(keywordIdToUpdate1, PHRASE_3)));

        ImmutableMap<Integer, List<TrafaretBidItem>> trafaretBidsByIndexMap = operation.getTrafaretBidsByIndexMap();

        assertThat(trafaretBidsByIndexMap,
                beanDiffer(singletonMap(0, defaultBsTrafaretAuctionData(null).getBidItems())));
    }

    @Test(expected = IllegalStateException.class)
    public void getTrafaretBidsByIndexMap_CheckStateFailedBeforeExecution() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        operation.prepare();

        operation.getTrafaretBidsByIndexMap();
    }
}
