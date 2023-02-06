package ru.yandex.market.crm.platform.reducers;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.CartEventType;
import ru.yandex.market.crm.platform.models.CartItem;
import ru.yandex.market.crm.platform.models.CartState;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartStateReducerTest {
    private static final String FACT_ID = "CartState";

    private final CartStateReducer reducer = new CartStateReducer();

    private static CartItem createCartItem(String itemId, int count, int price, long appendTs, long updateTs) {
        return CartItem.newBuilder()
                .setId(itemId)
                .setItemType("OFFER")
                .setWareMd5(UUID.randomUUID().toString())
                .setCount(count)
                .setPrice(price)
                .setAppendTime(appendTs)
                .setUpdateTime(updateTs)
                .build();
    }

    private static UserIds userIds() {
        return UserIds.newBuilder()
                .setPuid(12345)
                .build();
    }

    /**
     * При получении факта обновления корзины старый item в корзине должен замениться новым.
     * При этом время добавления товара в корзину должно остаться прежним.
     * Также общая стоимость товара в корзине должна пересчитаться
     */
    @Test
    public void testReduceForUpdateEvent() {
        var createTs = System.currentTimeMillis();

        var oldFact = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(createTs)
                .setUpdateTime(createTs)
                .setItemsTotal(2)
                .setPriceTotal(110)
                .addItems(createCartItem("123", 2, 10, createTs, createTs))
                .addItems(createCartItem("456", 1, 100, createTs, createTs))
                .setLastEventType(CartEventType.CREATE)
                .build();

        var updateTs = createTs + 100;
        var updatedItem = createCartItem("123", 10, 50, updateTs, updateTs);

        var newFact = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(updateTs)
                .setUpdateTime(updateTs)
                .setItemsTotal(1)
                .setPriceTotal(50)
                .addItems(updatedItem)
                .setLastEventType(CartEventType.UPDATE)
                .build();

        var result = reduce(List.of(oldFact), List.of(newFact));
        Collection<CartState> added = result.getAdded(FACT_ID);

        assertEquals(1, added.size());

        var resultFact = added.iterator().next();

        var expectedFact = oldFact.toBuilder()
                .setUpdateTime(updateTs)
                .setPriceTotal(150)
                .setItems(0, updatedItem.toBuilder()
                        .setAppendTime(createTs)
                        .build())
                .setLastEventType(CartEventType.UPDATE)
                .build();
        assertFacts(expectedFact, resultFact);
    }

    /**
     * При получении факта удаления товара из корзины item должен быть удален из корзины.
     * При этом общая стоимость товаров и количество товарных позиций должны быть обновлены
     */
    @Test
    public void testReduceForDeleteEvent() {
        var createTs = System.currentTimeMillis();
        var userIds = UserIds.newBuilder().setPuid(123).build();

        var oldFact = CartState.newBuilder()
                .setUserIds(userIds)
                .setTimestamp(createTs)
                .setUpdateTime(createTs)
                .setItemsTotal(2)
                .setPriceTotal(110)
                .addItems(createCartItem("123", 2, 10, createTs, createTs))
                .addItems(createCartItem("456", 1, 100, createTs, createTs))
                .setLastEventType(CartEventType.CREATE)
                .build();

        var newFactTs = createTs + 100;

        var newFact = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(newFactTs)
                .setUpdateTime(newFactTs)
                .setItemsTotal(1)
                .setPriceTotal(10)
                .addItems(createCartItem("123", 2, 50, newFactTs, newFactTs))
                .setLastEventType(CartEventType.DELETE)
                .build();

        var result = reduce(List.of(oldFact), List.of(newFact));
        Collection<CartState> added = result.getAdded(FACT_ID);

        assertEquals(1, added.size());

        var resultFact = added.iterator().next();

        var expectedFact = oldFact.toBuilder()
                .setUpdateTime(newFactTs)
                .setPriceTotal(100)
                .setItemsTotal(1)
                .removeItems(0)
                .setLastEventType(CartEventType.DELETE)
                .build();
        assertFacts(expectedFact, resultFact);
    }

    /**
     * При обработке нескольких фактов редьюс должен быть корректно выполнен
     */
    @Test
    public void testReduceForMultipleEvents() {
        var createTs = System.currentTimeMillis();
        var userIds = UserIds.newBuilder().setPuid(123).build();

        var oldFact = CartState.newBuilder()
                .setUserIds(userIds)
                .setTimestamp(createTs)
                .setUpdateTime(createTs)
                .setItemsTotal(2)
                .setPriceTotal(20)
                .addItems(createCartItem("123", 2, 10, createTs, createTs))
                .addItems(createCartItem("456", 3, 10, createTs, createTs))
                .setLastEventType(CartEventType.CREATE)
                .build();

        var newFactTs = createTs + 100;
        var addedItem = createCartItem("789", 1, 100, createTs, createTs);
        var updatedItem = createCartItem("123", 5, 25, createTs, createTs);

        var newFact1 = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(newFactTs)
                .setUpdateTime(newFactTs)
                .setItemsTotal(1)
                .setPriceTotal(100)
                .addItems(addedItem)
                .setLastEventType(CartEventType.CREATE)
                .build();

        var newFact2 = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(newFactTs)
                .setUpdateTime(newFactTs)
                .setItemsTotal(1)
                .setPriceTotal(25)
                .addItems(updatedItem)
                .setLastEventType(CartEventType.UPDATE)
                .build();

        var newFact3 = CartState.newBuilder()
                .setUserIds(userIds())
                .setTimestamp(newFactTs)
                .setUpdateTime(newFactTs)
                .setItemsTotal(1)
                .setPriceTotal(10)
                .addItems(createCartItem("456", 3, 10, newFactTs, newFactTs))
                .setLastEventType(CartEventType.DELETE)
                .build();

        var result = reduce(List.of(oldFact), List.of(newFact1, newFact2, newFact3));
        Collection<CartState> added = result.getAdded(FACT_ID);

        assertEquals(1, added.size());

        var resultFact = added.iterator().next();

        var expectedFact = oldFact.toBuilder()
                .setUpdateTime(newFactTs)
                .setPriceTotal(125)
                .setItemsTotal(2)
                .clearItems()
                .addItems(addedItem)
                .addItems(updatedItem.toBuilder()
                        .setAppendTime(createTs)
                        .build())
                .setLastEventType(CartEventType.DELETE)
                .build();
        assertFacts(expectedFact, resultFact);
    }

    private void assertFacts(CartState expected, CartState actual) {
        var expectedItems = new HashSet<>(expected.getItemsList());
        var actualItems = new HashSet<>(actual.getItemsList());

        assertEquals(expectedItems, actualItems);
        assertEquals(
                expected.toBuilder().clearItems().build(),
                actual.toBuilder().clearItems().build()
        );
    }

    private YieldMock reduce(List<CartState> stored, Collection<CartState> newFacts) {
        YieldMock mock = new YieldMock();
        reducer.reduce(stored, newFacts, mock);
        return mock;
    }
}
