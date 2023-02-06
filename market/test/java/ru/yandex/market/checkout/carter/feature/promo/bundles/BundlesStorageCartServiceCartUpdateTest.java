package ru.yandex.market.checkout.carter.feature.promo.bundles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.addItemTo;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneAsMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class BundlesStorageCartServiceCartUpdateTest extends CarterMockedDbTestBase {

    @Autowired
    private StorageCartService storageCartService;
    private UserContext userContext;
    private CartList basket;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong())));
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        basket.addItem(generateItem("some food offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some toys offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some car offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some strange offer", rnd.nextInt(1, 20)));
    }

    @Test
    public void updatingItemsShouldRemoveNotExistedItems() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> basketMap = cloneAsMap(basket);

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
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> basketMap = cloneAsMap(basket);

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
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        basket = assertCart(cloneCart(basket), extractBasketList(storageCartService.getListsOwnerId(userContext)));

        Map<Pair<String, String>, ItemOffer> basketMap = cloneAsMap(basket);

        basketMap.get(keyOf("some car offer")).setCount(10);
        basketMap.get(keyOf("some strange offer")).setCount(12);
        addItemTo(basketMap, generateItem("another strange offer", 3));
        addItemTo(basketMap, generateItem("another one strange offer", 5));

        CartList cartChange = asCart(userContext, basketMap);

        assertCart(cloneCart(cartChange), storageCartService.replaceCartListOwnerId(userContext,
                cartChange).getResult());

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));
    }

    @Test
    public void updatingItemsShouldFailOnNegativeCount() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

            basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

            Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

            changeSet.get(keyOf("some food offer")).setCount(-10);

            CartList cartChange = asCart(userContext, changeSet);

            storageCartService.replaceCartListOwnerId(userContext, cartChange);

            assertCart(cloneCart(cartChange), extractBasketList(storageCartService.getListsOwnerId(userContext)));
        });
    }

    @Test
    public void updatingItemsShouldSplitExistedBundlesIfNeed() {
        basket.addItem(generateItemWithBundle("some car offer", "some bundle", 2));
        basket.addItem(generateItemWithBundle("some strange offer", "some bundle", 3));
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        basket = assertCart(cloneCart(basket), extractBasketList(storageCartService.getListsOwnerId(userContext)));

        ItemOffer bundledItem = changeSet.remove(keyOf("some car offer", "some bundle"));
        assertNotNull(bundledItem);
        bundledItem.setBundleId(null);
        bundledItem.setBundlePromoId(null);

        addItemTo(changeSet, bundledItem);


        bundledItem = changeSet.remove(keyOf("some strange offer", "some bundle"));

        assertNotNull(bundledItem);
        bundledItem.setBundleId(null);
        bundledItem.setBundlePromoId(null);

        addItemTo(changeSet, bundledItem);

        basket = asCart(userContext, changeSet);
        storageCartService.replaceCartListOwnerId(userContext, basket);

        assertCart(cloneCart(basket), extractBasketList(storageCartService.getListsOwnerId(userContext)));
    }

    @Test
    public void updatingItemsShouldRecreateBundlesIfNeed() {
        basket.addItem(generateItemWithBundle("some car offer", "some bundle", 2));
        basket.addItem(generateItemWithBundle("some strange offer", "some bundle", 3));
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        basket = assertCart(cloneCart(basket), extractBasketList(storageCartService.getListsOwnerId(userContext)));

        ItemOffer bundledItem = changeSet.remove(keyOf("some car offer", "some bundle"));
        assertNotNull(bundledItem);
        bundledItem.setBundleId("another bundle");
        bundledItem.setCount(3);

        addItemTo(changeSet, bundledItem);

        changeSet.get(keyOf("some car offer")).setCount(10);


        bundledItem = changeSet.remove(keyOf("some strange offer", "some bundle"));

        assertNotNull(bundledItem);
        bundledItem.setBundleId("another bundle 2");
        bundledItem.setCount(2);

        addItemTo(changeSet, bundledItem);

        changeSet.get(keyOf("some strange offer")).setCount(12);

        basket = asCart(userContext, changeSet);
        storageCartService.replaceCartListOwnerId(userContext, basket);

        assertCart(cloneCart(basket), extractBasketList(storageCartService.getListsOwnerId(userContext)));
    }

    private CartList assertCart(CartList expected, CartList actual) {
        assertNotSame(expected, actual);
        Map<Pair<String, String>, ItemOffer> expectations = asMap(expected);
        Map<Pair<String, String>, ItemOffer> actualItem = asMap(actual);

        assertTrue(CollectionUtils.containsAll(actualItem.keySet(), expectations.keySet()));

        expectations.forEach((key, item) -> assertItem(item, actualItem.get(key)));
        return actual;
    }

    private void assertItem(ItemOffer expected, ItemOffer actual) {
        assertEquals(expected, actual);
        assertEquals(expected.getCount(), actual.getCount());
        assertEquals(expected.getBundlePromoId(), actual.getBundlePromoId());
        assertEquals(expected.getPrice(), actual.getPrice());
        assertEquals(expected.getName(), actual.getName());
    }
}
