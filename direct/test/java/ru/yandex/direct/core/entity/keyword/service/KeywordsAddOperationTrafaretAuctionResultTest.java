package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationTrafaretAuctionResultTest extends KeywordsAddOperationBaseTest {

    @Test
    public void getTrafaretBidsByIndexMap_AddNewKeyword_TrafaretBidsContainsResult() {
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        assumeThat(operation.prepareAndApply(), isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        ImmutableMap<Integer, List<TrafaretBidItem>> trafaretBidsByIndexMap = operation.getTrafaretBidsByIndexMap();

        assertThat(trafaretBidsByIndexMap,
                beanDiffer(singletonMap(0, defaultBsTrafaretAuctionData(null).getBidItems())));
    }

    @Test
    public void getTrafaretBidsByIndexMap_AddKeywordWithDuplicate_ReturnsTrafaretBidsOnlyByUniqueKeyword() {
        createOneActiveAdGroup();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_1));
        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        assumeThat(operation.prepareAndApply(), isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));

        ImmutableMap<Integer, List<TrafaretBidItem>> trafaretBidsByIndexMap = operation.getTrafaretBidsByIndexMap();

        assertThat(trafaretBidsByIndexMap,
                beanDiffer(singletonMap(0, defaultBsTrafaretAuctionData(null).getBidItems())));
    }

    @Test(expected = IllegalStateException.class)
    public void getTrafaretBidsByIndexMap_CheckStateFailedBeforeExecution() {
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        operation.prepare();

        operation.getTrafaretBidsByIndexMap();
    }
}
