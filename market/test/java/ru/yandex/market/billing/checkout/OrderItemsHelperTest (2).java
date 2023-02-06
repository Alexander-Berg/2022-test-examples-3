package ru.yandex.market.billing.checkout;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.core.order.model.MbiOrderItem;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Тесты на логику работы {@link OrderItemsHelper}.
 *
 * @author fbokovikov
 */
public class OrderItemsHelperTest {

    private static final MbiOrderItem FIRST_ITEM = MbiOrderItem.builder()
            .setOrderId(12L).setFeedId(12L).setOfferId("offer1")
            .setOfferName("offerName1").setModelId(1)
            .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
            .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
            .setFeedPrice(BigDecimal.TEN).setCount(5).setNormFee(BigDecimal.TEN)
            .setIntFee(15).setShopFee(BigDecimal.valueOf(20)).setNetFeeUE(BigDecimal.valueOf(25))
            .setPromos(Collections.emptyList())
            .build();

    private static final MbiOrderItem SECOND_ITEM = MbiOrderItem.builder()
            .setOrderId(12L).setFeedId(12L).setOfferId("offer2")
            .setOfferName("offerName2").setModelId(1)
            .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
            .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
            .setFeedPrice(BigDecimal.TEN).setCount(3).setNormFee(BigDecimal.valueOf(40))
            .setIntFee(45).setShopFee(BigDecimal.valueOf(50)).setNetFeeUE(BigDecimal.valueOf(55))
            .setPromos(Collections.emptyList())
            .build();

    private static final Collection<MbiOrderItem> ITEMS_BEFORE = ImmutableList.of(FIRST_ITEM, SECOND_ITEM);

    @Test
    public void testFilteringByCountSameCollection() {
        Collection<MbiOrderItem> diffByCount = OrderItemsHelper.getDiffByCount(ITEMS_BEFORE, ITEMS_BEFORE);
        assertTrue(diffByCount.isEmpty());
    }

    @Test
    public void testFilteringByCountEmptyNewItems() {
        Collection<MbiOrderItem> diffByCount = OrderItemsHelper.getDiffByCount(ITEMS_BEFORE, Collections.EMPTY_LIST);
        assertTrue(diffByCount.isEmpty());
    }

    @Test
    public void testFilteringByCountPositiveScenario() {
        MbiOrderItem item1 = MbiOrderItem.builder()
                .setOrderId(12L).setFeedId(12L).setOfferId("offer1")
                .setOfferName("offerName1").setModelId(1)
                .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                .setFeedPrice(BigDecimal.TEN).setCount(4).setNormFee(BigDecimal.TEN)
                .setIntFee(15).setShopFee(BigDecimal.valueOf(20)).setNetFeeUE(BigDecimal.valueOf(25))
                .setPromos(Collections.emptyList())
                .build();

        MbiOrderItem item2 = MbiOrderItem.builder()
                .setOrderId(12L).setFeedId(12L).setOfferId("offer2")
                .setOfferName("offerName2").setModelId(1)
                .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                .setFeedPrice(BigDecimal.TEN).setCount(3).setNormFee(BigDecimal.valueOf(40))
                .setIntFee(45).setShopFee(BigDecimal.valueOf(50)).setNetFeeUE(BigDecimal.valueOf(55))
                .setPromos(Collections.emptyList())
                .build();

        Collection<MbiOrderItem> newItems = Arrays.asList(item1, item2);
        Collection<MbiOrderItem> diffByCount = OrderItemsHelper.getDiffByCount(ITEMS_BEFORE, newItems);
        assertFalse(diffByCount.isEmpty());
        assertTrue(diffByCount.size() == 1);
        assertThat(diffByCount, hasItem(item1));
    }

    @Test
    public void testSearchingDeletedItemsAllDeleted() {
        Collection<MbiOrderItem> deletedItems = OrderItemsHelper.getDeletedItems(ITEMS_BEFORE, Collections.EMPTY_LIST);
        assertTrue(!deletedItems.isEmpty());
        assertThat(deletedItems, containsInAnyOrder(FIRST_ITEM, SECOND_ITEM));
    }

    @Test
    public void testSearchingDeletedItemsNoneDeleted() {
        Collection<MbiOrderItem> deletedItems = OrderItemsHelper.getDeletedItems(ITEMS_BEFORE, ITEMS_BEFORE);
        assertTrue(deletedItems.isEmpty());
    }

    @Test
    public void testSearchingDeletedItemsOneDeleted() {
        Collection<MbiOrderItem> deletedItems = OrderItemsHelper.getDeletedItems(ITEMS_BEFORE, Arrays.asList(FIRST_ITEM));
        assertFalse(deletedItems.isEmpty());
        assertTrue(deletedItems.size() == 1);
        assertThat(deletedItems, hasItem(SECOND_ITEM));
    }

}
