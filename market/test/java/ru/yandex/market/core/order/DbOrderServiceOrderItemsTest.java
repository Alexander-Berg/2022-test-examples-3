package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.OrderItemForBilling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Тесты методов {@link DbOrderService} для работы с {@link MbiOrderItem}.
 */
@DbUnitDataSet(before = "db/datasource.csv")
class DbOrderServiceOrderItemsTest extends FunctionalTest {

    private static final long ORDER_ID = 12L;
    @Autowired
    private DbOrderService orderService;

    /**
     * Вспомогательный метод для проверки NotNull полей.
     */
    private static void assertNonNullableItemContent(MbiOrderItem actualItem) {
        assertThat("OrderId check failed", actualItem.getOrderId(), is(12L));
        assertThat("FeedId check failed", actualItem.getFeedId(), is(100500L));
        assertThat("OfferId check failed", actualItem.getOfferId(), is("someOfferId"));
        assertThat("OfferName check failed", actualItem.getOfferName(), is("someOfferName"));
        assertThat("WareMd5 check failed", actualItem.getWareMd5(), is("qwerty"));
        assertThat("CategoryId check failed", actualItem.getCategoryId(), is(700));
        assertThat("FeedCategoryId check failed", actualItem.getFeedCategoryId(), is(new BigInteger("99999")));
        assertThat("ShowUid check failed", actualItem.getShowUid(), is("showUid"));
        assertThat("Price check failed", actualItem.getPrice(), comparesEqualTo(new BigDecimal("500.1")));
        assertThat("BuyerPrice check failed", actualItem.getBuyerPrice(), comparesEqualTo(new BigDecimal("500.2")));
        assertThat("Count check failed", actualItem.getCount(), is(5));
        assertThat("NormFee check failed", actualItem.getNormFee(), comparesEqualTo(new BigDecimal("12.5")));
        assertThat("IntFee check failed", actualItem.getIntFee(), is(100));
        assertThat("GrossFeeUE check failed", actualItem.getShopFee(), comparesEqualTo(new BigDecimal("1260")));
        assertThat("NetFeeUE check failed", actualItem.getNetFeeUE(), comparesEqualTo(new BigDecimal("12.7")));
        assertThat("Subsidy check failed", actualItem.getSubsidy(), comparesEqualTo(new BigDecimal("12.8")));
        assertThat("Promos check failed", actualItem.getPromos(), IsEmptyCollection.empty());
        assertThat("Fulfillment shop id check failed", actualItem.getFulfilmentShopId(), is(774L));
        assertThat("Fulfillment supplier id check failed", actualItem.getFfSupplierId(), nullValue());
    }

    /**
     * Поднимаем из базы, когда все поля !=null.
     * Значения в cpa_order не имеют значения, важно только наличие заказа с id, для выполнени conatraint'а.
     */
    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_oneItem.before.csv"
    )
    void test_getOrderItems_when_hasAllColumns_should_returnExpected() {
        Collection<MbiOrderItem> actualItems = orderService.getOrderItems(ORDER_ID);
        assertThat(actualItems, hasSize(1));
        MbiOrderItem actualItem = actualItems.iterator().next();

        assertThat("ModelId check failed", actualItem.getModelId(), is(1234));
        assertThat("Msku check failed", actualItem.getMsku(), is(10_000_000_000L));
        assertNonNullableItemContent(actualItem);
    }

    /**
     * Поднимаем из базы, когда nullable выставлены в null.
     */
    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_oneItem_withNullableColumns.before.csv"
    )
    void test_getOrderItems_when_hasNullColumnValues_should_returnExpected() {
        Collection<MbiOrderItem> actualItems = orderService.getOrderItems(ORDER_ID);
        assertThat(actualItems, hasSize(1));
        MbiOrderItem actualItem = actualItems.iterator().next();

        assertThat("ModelId check failed", actualItem.getModelId(), nullValue());
        assertThat("Msku check failed", actualItem.getMsku(), nullValue());
        assertNonNullableItemContent(actualItem);
    }

    /**
     * Когда в базе нет позиций, метод должен возвращать пустую коллекцию а не null.
     * Значения в cpa_order не имеют значения, важно только наличие заказа с id, для выполнени conatraint'а.
     */
    @Test
    @DbUnitDataSet
    void test_getOrderItems_when_hasNone_should_returnEmptyCollection() {
        Collection<MbiOrderItem> actualItems = orderService.getOrderItems(ORDER_ID);
        assertThat(actualItems, IsEmptyCollection.empty());
    }

    /**
     * Когда в базе несколько позиций, метод должен возвращать ожидаему коллекцию.
     * Значения в cpa_order не имеют значения, важно только наличие заказа с id, для выполнени conatraint'а.
     */
    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_multipleItems.before.csv"
    )
    void test_getOrderItems_when_hasMultipleItems_should_returnExpectedCollection() {
        Collection<MbiOrderItem> actualItems = orderService.getOrderItems(ORDER_ID);
        assertThat(actualItems, hasSize(2));
    }

    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_getOrderItemsByAnaplanPromoId.before.csv"
    )
    void test_getOrderItemsByAnaplanPromoId() {
        Collection<MbiOrderItem> actualItems = orderService.getOrderItemsByAnaplanPromoId(10297174L, "#2363", false);
        assertThat(actualItems, hasSize(1));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(8122327L)
        );

        actualItems = orderService.getOrderItemsByAnaplanPromoId(10297177L, "#2362", false);
        assertThat(actualItems, is(empty()));

        actualItems = orderService.getOrderItemsByAnaplanPromoId(10297174L, "#2362", false);
        assertThat(actualItems, hasSize(3));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(8122329L, 8122356L, 8122399L)
        );

        actualItems = orderService.getOrderItemsByAnaplanPromoId(10812050L, "#8123", true);
        assertThat(actualItems, hasSize(1));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(9685696L)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_getOrderItemsByPartnerPromoId.before.csv"
    )
    void test_getOrderItemsByPartnerPromoId() {
        Collection<MbiOrderItem> actualItems =
                orderService.getOrderItemsByPartnerPromoId(10297174L, "10297174_RTDGBEG", false);
        assertThat(actualItems, hasSize(3));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(8122327L, 8122328L, 8122395L)
        );

        actualItems =
                orderService.getOrderItemsByPartnerPromoId(10297175L, "10297174_RTDGBEG", false);
        assertThat(actualItems, is(empty()));

        actualItems =
                orderService.getOrderItemsByPartnerPromoId(10297174L, "10297174_TYYYHDN", false);
        assertThat(actualItems, hasSize(1));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(8122389L)
        );

        actualItems =
                orderService.getOrderItemsByPartnerPromoId(10297175L, "10297175_TYYRTSN", false);
        assertThat(actualItems, is(empty()));

        actualItems =
                orderService.getOrderItemsByPartnerPromoId(10812050L, "10812050_DKGAKENW", true);
        assertThat(actualItems, hasSize(1));
        assertThat(
                actualItems.stream()
                        .map(MbiOrderItem::getId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(9685696L)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemsTest_getOrderItemForBilling_DSBS_order.before.csv"
    )
    void test_getOrderItemForBilling_DSBS_order() {
        List<OrderItemForBilling> actual = orderService.getOrderItemsForBilling(List.of(1L, 2L));

        assertThat(actual, Matchers.hasSize(2));
        Assertions.assertEquals(
                List.of(774L, 774L),
                actual.stream()
                        .map(OrderItemForBilling::getSupplierId)
                        .collect(Collectors.toList())
        );
    }
}
