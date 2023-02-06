package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageOrderServiceHelperTest {

    @Test
    public void shouldGetNewCountWhenOnlyNewCountIsDifferentForIntegerItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 3;
        BigDecimal newQuantity = BigDecimal.valueOf(2);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(3);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewCountWhenOnlyNewCountIsDifferentForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(4.5);

        int newCount = 2;
        BigDecimal newQuantity = BigDecimal.valueOf(4.5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(2);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewCountWhenOnlyNewCountIsDifferentAndEqualsMagicOne() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 1;
        BigDecimal newQuantity = BigDecimal.valueOf(2);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(1);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetZeroWhenOnlyNewCountIsDifferentAndEqualsMagicZeroForIntegerItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 0;
        BigDecimal newQuantity = BigDecimal.valueOf(2);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(0);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetZeroWhenNewCountEqualsMagicZeroForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(3.1);

        int newCount = 0;
        BigDecimal newQuantity = BigDecimal.valueOf(3.1);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(0);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetZeroWhenOnlyNewQuantityIsDifferentAndEqualsMagicZeroForMeasureItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2.3);

        int newCount = 2;
        BigDecimal newQuantity = BigDecimal.valueOf(0);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(0);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewQuantityWhenOnlyNewQuantityIsDifferentForIntegerItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 2;
        BigDecimal newQuantity = BigDecimal.valueOf(2.5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(2.5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewQuantityWhenOnlyNewQuantityIsDifferentForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(4.5);

        int newCount = 1;
        BigDecimal newQuantity = BigDecimal.valueOf(5.5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(5.5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewQuantityWhenNewCountAndNewQuantityAreEqualForIntegerItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 3;
        BigDecimal newQuantity = BigDecimal.valueOf(3);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(3);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewQuantityWhenNewCountAndNewQuantityAreEqualForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(4.5);

        int newCount = 5;
        BigDecimal newQuantity = BigDecimal.valueOf(5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewQuantityWhenChangeRequestWasCastledByMeasureCase() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 1;
        BigDecimal newQuantity = BigDecimal.valueOf(2.5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(2.5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldThrowExceptionWhenCountAndQuantityAreDifferentAtTheSameTimeForIntegerItem() {
        int count = 2;
        BigDecimal quantity = BigDecimal.valueOf(2);

        int newCount = 3;
        BigDecimal newQuantity = BigDecimal.valueOf(2.5);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");

        OrderItemsException exception = assertThrows(OrderItemsException.class,
                () -> StorageOrderServiceHelper.getNewQuantity(item,
                        orderItemChangeRequest));
        assertThat(exception.getCode(), is(OrderItemsException.ITEM_HAS_AMBIGUOUS_QUANTITY));
    }


    @Test
    public void shouldThrowExceptionWhenCountAndQuantityAreDifferentAtTheSameTimeForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(4.5);

        int newCount = 3;
        BigDecimal newQuantity = BigDecimal.valueOf(4.2);

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");

        OrderItemsException exception = assertThrows(OrderItemsException.class,
                () -> StorageOrderServiceHelper.getNewQuantity(item,
                        orderItemChangeRequest));
        assertThat(exception.getCode(), is(OrderItemsException.ITEM_HAS_AMBIGUOUS_QUANTITY));

    }

    @Test
    public void shouldGetNewCountWhenNewQuantityIsNullForMeasureItem() {
        int count = 1;
        BigDecimal quantity = BigDecimal.valueOf(4.5);

        int newCount = 5;
        BigDecimal newQuantity = null;

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldGetNewCountWhenNewQuantityIsNullForIntegerItem() {
        int count = 4;
        BigDecimal quantity = BigDecimal.valueOf(4);

        int newCount = 5;
        BigDecimal newQuantity = null;

        OrderItem item = OrderItemProvider.orderItemBuilder().count(count).quantity(quantity).build();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(11L, newCount, newQuantity,
                44L, "test");
        BigDecimal actualQuantity = StorageOrderServiceHelper.getNewQuantity(item, orderItemChangeRequest);
        BigDecimal expectedQuantity = BigDecimal.valueOf(5);
        assertThat(actualQuantity, is(expectedQuantity));
    }

    @Test
    public void shouldCalcIncreasedDiffButNotAffectOriginalItem() {
        Order order = OrderProvider.getBlueOrder();
        OrderItem originalItem = order.getItems().iterator().next();
        originalItem.setCount(3);
        originalItem.setValidIntQuantity(3);
        originalItem.setId(1L);
        BigDecimal oldQuantity = originalItem.getQuantity();
        BigDecimal newQuantity = BigDecimal.valueOf(13);
        BigDecimal expectedIncrease = newQuantity.subtract(oldQuantity);

        OrderItemChangeRequest changeRequest = new OrderItemChangeRequest(originalItem.getId(),
                newQuantity.intValue(), newQuantity,
                originalItem.getFeedId(), originalItem.getOfferId());
        OrderItemsChanges changes = StorageOrderServiceHelper.findItemsChanges(order, List.of(changeRequest));

        assertEquals(1, changes.getIncreasedItemsDiff().size());
        assertEquals(expectedIncrease, changes.getIncreasedItemsDiff().iterator().next().getQuantity());
        assertEquals(newQuantity, originalItem.getQuantity());
        assertEquals(newQuantity.intValue(), originalItem.getCount());
    }
}
