package ru.yandex.market.checkout.carter.storage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class StorageCartServiceCartListUpdateTest extends CarterMockedDbTestBase {

    @Autowired
    private StorageCartService storageCartService;
    private UserContext userContext;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong()))
        );
    }

    @Test
    public void updatingItemsShouldRemoveNotExistedItems() {
        CartList basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basket.getItems().add(generateItem("some food offer"));
        basket.getItems().add(generateItem("some toys offer"));
        basket.getItems().add(generateItem("some car offer"));
        basket.getItems().add(generateItem("some strange offer"));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> basketMap = asMap(basket);

        assertNotNull(basketMap.remove(keyOf("some toys offer")));
        assertNotNull(basketMap.remove(keyOf("some food offer")));

        CartList cartChange = asCart(userContext, basketMap);

        storageCartService.replaceCartListOwnerId(userContext, cartChange);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basketMap = asMap(basket);

        assertTrue(basketMap.containsKey(keyOf("some car offer")));
        assertTrue(basketMap.containsKey(keyOf("some strange offer")));
        assertEquals(2, basketMap.size());
    }

    @Test
    public void updatingItemsShouldAddNewItems() {
        CartList basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basket.addItem(generateItem("some food offer"));
        basket.addItem(generateItem("some toys offer"));
        basket.addItem(generateItem("some car offer"));
        basket.addItem(generateItem("some strange offer"));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> basketMap = asMap(basket);

        assertNotNull(basketMap.remove(keyOf("some toys offer")));
        assertNotNull(basketMap.remove(keyOf("some food offer")));

        CartList cartChange = asCart(userContext, basketMap);

        cartChange.addItem(generateItem("another strange offer"));
        cartChange.addItem(generateItem("another one strange offer"));

        storageCartService.replaceCartListOwnerId(userContext, cartChange);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basketMap = asMap(basket);

        assertEquals(4, basketMap.size());
        assertTrue(basketMap.containsKey(keyOf("some car offer")));
        assertTrue(basketMap.containsKey(keyOf("some strange offer")));
        assertTrue(basketMap.containsKey(keyOf("another strange offer")));
        assertTrue(basketMap.containsKey(keyOf("another one strange offer")));
    }

    @Test
    public void updatingItemsShouldChangeCountOfItems() {
        CartList basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basket.addItem(generateItem("some food offer"));
        basket.addItem(generateItem("some toys offer"));
        basket.addItem(generateItem("some car offer"));
        basket.addItem(generateItem("some strange offer"));

        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> basketMap = asMap(basket);

        assertNotNull(basketMap.remove(keyOf("some toys offer")));
        assertNotNull(basketMap.remove(keyOf("some food offer")));

        assertEquals(Integer.valueOf(1), basketMap.get(keyOf("some car offer")).getCount());
        assertEquals(Integer.valueOf(1), basketMap.get(keyOf("some strange offer")).getCount());

        basketMap.get(keyOf("some car offer")).setCount(10);
        basketMap.get(keyOf("some strange offer")).setCount(12);

        CartList cartChange = asCart(userContext, basketMap);

        cartChange.addItem(generateItem("another strange offer", 3));
        cartChange.addItem(generateItem("another one strange offer", 5));

        storageCartService.replaceCartListOwnerId(userContext, cartChange);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basketMap = asMap(basket);

        assertEquals(4, basketMap.size());
        assertTrue(basketMap.containsKey(keyOf("some car offer")));
        assertTrue(basketMap.containsKey(keyOf("some strange offer")));
        assertTrue(basketMap.containsKey(keyOf("another strange offer")));
        assertTrue(basketMap.containsKey(keyOf("another one strange offer")));

        assertEquals(Integer.valueOf(10), basketMap.get(keyOf("some car offer")).getCount());
        assertEquals(Integer.valueOf(12), basketMap.get(keyOf("some strange offer")).getCount());
        assertEquals(Integer.valueOf(3), basketMap.get(keyOf("another strange offer")).getCount());
        assertEquals(Integer.valueOf(5), basketMap.get(keyOf("another one strange offer")).getCount());
    }

    private ItemOffer generateItem(String offerId) {
        ItemOffer item = new ItemOffer(offerId, offerId);
        item.setShopId(155L);
        item.setMsku(1L);
        item.setHid(124534242L);
        item.setPrice(BigDecimal.TEN);
        return item;
    }

    private ItemOffer generateItem(String offerId, int count) {
        ItemOffer item = generateItem(offerId);
        item.setCount(count);
        return item;
    }
}
