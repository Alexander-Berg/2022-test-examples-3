package ru.yandex.market.ff.service.implementation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WithdrawRequestSplittingServiceUnitTest extends SoftAssertionSupport {

    @Mock
    private ConcreteEnvironmentParamService environmentParamService;

    private WithdrawRequestSplittingService withdrawRequestSplittingService;

    @BeforeEach
    public void init() {
        when(environmentParamService.getMaxWithdrawItemsTotalCount()).thenReturn(1000);
        withdrawRequestSplittingService = new WithdrawRequestSplittingService(environmentParamService);
    }

    @Test
    public void splitWhenSumCountIsLessThanMax() {
        List<RequestItem> items = List.of(
                createItem("art1", 10),
                createItem("art2", 20),
                createItem("art3", 950)
        );
        List<List<RequestItem>> splits = withdrawRequestSplittingService.splitByMaxItemsCountInWithdraw(items);
        assertions.assertThat(splits).hasSize(1);
        assertions.assertThat(splits.get(0)).containsExactlyElementsOf(items);
    }

    @Test
    public void splitWhenSumCountIsEqualToMax() {
        List<RequestItem> items = List.of(
                createItem("art1", 10),
                createItem("art2", 20),
                createItem("art3", 970)
        );
        List<List<RequestItem>> splits = withdrawRequestSplittingService.splitByMaxItemsCountInWithdraw(items);
        assertions.assertThat(splits).hasSize(1);
        assertions.assertThat(splits.get(0)).containsExactlyElementsOf(items);
    }

    @Test
    public void splitWhenSumCountIsGreaterThanMaxAndExistsBigSplits() {
        List<RequestItem> items = List.of(
                createItem("art1", 10),
                createItem("art2", 20),
                createItem("art3", 2970)
        );
        List<List<RequestItem>> expectedSplits = List.of(
                List.of(createItem("art3", 1000)),
                List.of(createItem("art3", 1000)),
                List.of(createItem("art1", 10), createItem("art2", 20), createItem("art3", 970))
        );
        List<List<RequestItem>> splits = withdrawRequestSplittingService.splitByMaxItemsCountInWithdraw(items);
        assertSplitsEquals(expectedSplits, splits);
    }

    @Test
    public void splitWhenSumCountIsGreaterThanMaxAndExistsSmallSplits() {
        List<RequestItem> items = List.of(
                createItem("art1", 10),
                createItem("art2", 20),
                createItem("art3", 990),
                createItem("art4", 250),
                createItem("art5", 735),
                createItem("art6", 735)
        );
        List<List<RequestItem>> expectedSplits = List.of(
                List.of(createItem("art1", 10), createItem("art3", 990)),
                List.of(createItem("art4", 250), createItem("art6", 735)),
                List.of(createItem("art2", 20), createItem("art5", 735))
        );
        List<List<RequestItem>> splits = withdrawRequestSplittingService.splitByMaxItemsCountInWithdraw(items);
        assertSplitsEquals(expectedSplits, splits);
    }

    @Test
    public void splitWhenCountOfSomeItemIsMax() {
        List<RequestItem> items = List.of(
                createItem("art1", 1000),
                createItem("art2", 1000),
                createItem("art3", 980),
                createItem("art4", 20)
        );
        List<List<RequestItem>> expectedSplits = List.of(
                List.of(createItem("art1", 1000)),
                List.of(createItem("art2", 1000)),
                List.of(createItem("art3", 980), createItem("art4", 20))
        );
        List<List<RequestItem>> splits = withdrawRequestSplittingService.splitByMaxItemsCountInWithdraw(items);
        assertSplitsEquals(expectedSplits, splits);
    }

    private void assertSplitsEquals(List<List<RequestItem>> expectedSplits, List<List<RequestItem>> actualSplits) {
        assertions.assertThat(actualSplits).hasSize(expectedSplits.size());
        Set<Integer> indexes = new HashSet<>();
        for (List<RequestItem> split : actualSplits) {
            for (int i = 0; i < expectedSplits.size(); i++) {
                if (indexes.contains(i)) {
                    continue;
                }
                List<RequestItem> expectedSplit = expectedSplits.get(i);
                boolean splitsEquals = false;
                if (split.size() == expectedSplit.size()) {
                    Set<Integer> equalityIndexes = new HashSet<>();
                    for (RequestItem item : split) {
                        for (int j = 0; j < expectedSplit.size(); j++) {
                            if (equalityIndexes.contains(j)) {
                                continue;
                            }
                            RequestItem expectedItem = expectedSplit.get(j);
                            if (item.getArticle().equals(expectedItem.getArticle())) {
                                equalityIndexes.add(j);
                                break;
                            }
                        }
                    }
                    splitsEquals = equalityIndexes.size() == split.size();
                }
                if (splitsEquals) {
                    indexes.add(i);
                    break;
                }
            }
        }
    }

    private RequestItem createItem(String article, int count) {
        RequestItem item = new RequestItem();
        item.setArticle(article);
        item.setCount(count);
        item.setBarcodes(List.of());
        item.setMarketBarcodes(List.of());
        item.setMarketVendorCodes(List.of());
        item.setRequestItemCargoTypes(Set.of());
        return item;
    }
}
