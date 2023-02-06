package ru.yandex.market.checkout.carter.feature.promo.bundles;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.BUNDLE_NULL;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneItemsWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateSomeBundles;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateSomeItems;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.groupByBundles;

public class BundlesStorageCartServiceUniqTest extends CarterMockedDbTestBase {

    private static final String OBJECT_ID = "offer-md5";
    @Autowired
    private StorageCartService storageCartService;
    private UserContext owner;

    @BeforeEach
    public void prepare() {
        owner = UserContext.of(OwnerKey.of(
                BLUE, UserIdType.UUID, "" + Math.abs(ThreadLocalRandom.current().nextLong())));
    }

    @Test
    public void mergingOfBundledItemsShouldPassSeparately() {
        int itemWithoutBundles = 5;
        int itemWithBundles = 4;

        while (itemWithoutBundles-- > 0) {
            storageCartService.createOrReplaceItemByMsku(owner, generateItem(OBJECT_ID));
        }

        while (itemWithBundles-- > 0) {
            storageCartService.createOrReplaceItemByMsku(owner, generateItemWithBundle(OBJECT_ID, "some-promo-bundle"));
        }

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertEquals(2, actual.getItems().stream().filter(Objects::nonNull).count());

        Multimap<String, CartItem> cartBundles = groupByBundles(actual.getItems());

        assertEquals(1, cartBundles.get(BUNDLE_NULL).size());
        assertEquals(1, cartBundles.get("some-promo-bundle").size());

        assertEquals(Integer.valueOf(1), cartBundles.get(BUNDLE_NULL).iterator().next().getCount());
        assertEquals(Integer.valueOf(1), cartBundles.get("some-promo-bundle").iterator().next().getCount());

    }

    @Test
    public void batchMergingOfBundledItemsShouldPassSeparately() {
        int itemWithoutBundles = 5;
        int itemWithBundles = 4;

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        while (itemWithoutBundles-- > 0) {
            actual.addItem(generateItem(OBJECT_ID));
        }

        while (itemWithBundles-- > 0) {
            actual.addItem(generateItemWithBundle(OBJECT_ID, "some-promo-bundle"));
        }

        storageCartService.bulkUpdateItemsOwnerId(owner, List.of(actual), true);

        actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertEquals(2, actual.getItems().stream().filter(Objects::nonNull).count());

        Multimap<String, CartItem> cartBundles = groupByBundles(actual.getItems());

        assertEquals(1, cartBundles.get(BUNDLE_NULL).size());
        assertEquals(1, cartBundles.get("some-promo-bundle").size());

        assertEquals(Integer.valueOf(1), cartBundles.get(BUNDLE_NULL).iterator().next().getCount());
        assertEquals(Integer.valueOf(1), cartBundles.get("some-promo-bundle").iterator().next().getCount());

    }

    @Test
    public void updateCountForItemsInBundlesShouldPassSeparately() {
        Long cartItemId = storageCartService.createOrReplaceItemByMsku(owner, generateItem(OBJECT_ID)).getResult();
        Long cartItemInBundleId = storageCartService.createOrReplaceItemByMsku(owner,
                generateItemWithBundle(OBJECT_ID, "some-promo-bundle")).getResult();

        storageCartService.updateItemCountOwnerId(owner, cartItemId, 5);

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertEquals(2, actual.getItems().stream().filter(Objects::nonNull).count());

        Multimap<String, CartItem> cartBundles = groupByBundles(actual.getItems());

        assertEquals(Integer.valueOf(5), cartBundles.get(BUNDLE_NULL).iterator().next().getCount());
        assertEquals(Integer.valueOf(1), cartBundles.get("some-promo-bundle").iterator().next().getCount());

        storageCartService.updateItemCountOwnerId(owner, cartItemInBundleId, 6);

        actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertEquals(2, actual.getItems().stream().filter(Objects::nonNull).count());

        cartBundles = groupByBundles(actual.getItems());

        assertEquals(Integer.valueOf(5), cartBundles.get(BUNDLE_NULL).iterator().next().getCount());
        assertEquals(Integer.valueOf(6), cartBundles.get("some-promo-bundle").iterator().next().getCount());
    }

    @Test
    public void updateCountForItemsWithDifferentBundlesShouldPassSeparately() {
        Set<CartItem> cartItems = Stream.concat(generateSomeItems(20),
                generateSomeBundles(5, 5))
                .peek(generated ->
                        generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()))
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));

        Map<CartItem, Integer> itemCountMap = cartItems.stream().map(item -> {
            int count = ThreadLocalRandom.current().nextInt(0, 10);
            storageCartService.updateItemCountOwnerId(owner, item.getId(), count);
            return Pair.of(item, count);
        }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));
        Map<Long, CartItem> cartResultItemMap = actual.getItems().stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));
        assertEquals(cartItems.size(), actual.getItems().stream().filter(Objects::nonNull).count());

        itemCountMap.forEach((key, value) -> {
            CartItem result = cartResultItemMap.get(key.getId());

            assertNotNull(result);
            assertEquals(value, result.getCount());
        });
    }

    @Test
    public void replaceItemsWithMskuInBundlesMustShouldPassSeparately() {
        Long sameMsku = ThreadLocalRandom.current().nextLong(0, 1000);
        Set<ItemOffer> cartItems = Stream.concat(generateSomeItems(5),
                generateSomeBundles(2, 3))
                .peek(generated ->
                        generated.setMsku(sameMsku))
                .peek(generated ->
                        generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()))
                .collect(Collectors.toCollection(HashSet::new));
        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));
        assertEquals(3, actual.getItems().stream()
                .map(ItemOffer.class::cast)
                .peek(item -> assertEquals(sameMsku, item.getMsku())).count());
    }

    @Test
    public void deletingItemsWithoutBundlesShouldNotAffectBundledItems() {
        String someBundleId = UUID.randomUUID().toString();
        Set<ItemOffer> cartItems = cloneItemsWith(generateSomeItems(5), offer -> {
            offer.setBundleId(someBundleId);
            offer.setBundlePromoId(md5Hex(someBundleId));
        }).collect(Collectors.toCollection(HashSet::new));

        cartItems.forEach(generated ->
                generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()));

        Multimap<String, CartItem> bundledItems = groupByBundles(cartItems);

        assertEquals(2, bundledItems.keySet().size());

        Collection<CartItem> itemsWithoutBundles = bundledItems.get(BUNDLE_NULL);

        assertTrue(CollectionUtils.isNotEmpty(itemsWithoutBundles));
        assertTrue(CollectionUtils.isNotEmpty(bundledItems.get(someBundleId)));

        itemsWithoutBundles.forEach(item -> storageCartService.deleteItem(owner, item.getId()));

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));
        assertTrue(CollectionUtils.isNotEmpty(actual.getItems()));
        assertFalse(CollectionUtils.containsAny(actual.getItems(), itemsWithoutBundles));
        assertTrue(CollectionUtils.containsAll(actual.getItems(), bundledItems.get(someBundleId)));
    }

    @Test
    public void deletingBundledItemsShouldNotAffectOtherItems() {
        String someBundleId = UUID.randomUUID().toString();
        String anotherBundle = UUID.randomUUID().toString();
        Set<ItemOffer> prototypeList = generateSomeItems(5).collect(Collectors.toSet());
        Set<ItemOffer> cartItems = Stream.concat(
                cloneItemsWith(prototypeList.stream(), offer -> {
                    offer.setBundleId(someBundleId);
                    offer.setBundlePromoId(md5Hex(someBundleId));
                }), cloneItemsWith(prototypeList.stream(), offer -> {
                    offer.setBundleId(anotherBundle);
                    offer.setBundlePromoId(md5Hex(offer.getBundleId()));
                })).collect(Collectors.toCollection(HashSet::new));

        cartItems.forEach(generated ->
                generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()));

        Multimap<String, CartItem> bundledItems = groupByBundles(cartItems);

        assertEquals(3, bundledItems.keySet().size());

        Collection<CartItem> itemsToRemove = bundledItems.get(someBundleId);

        assertTrue(CollectionUtils.isNotEmpty(itemsToRemove));

        itemsToRemove.forEach(item -> storageCartService.deleteItem(owner, item.getId()));

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));
        assertTrue(CollectionUtils.isNotEmpty(actual.getItems()));
        assertFalse(CollectionUtils.containsAny(actual.getItems(), itemsToRemove));
        assertTrue(CollectionUtils.containsAll(actual.getItems(), bundledItems.entries().stream()
                .filter(entry -> !someBundleId.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList())));
    }

    @Test
    public void batchDeletingItemsWithoutBundlesShouldNotAffectBundledItems() {
        String someBundleId = UUID.randomUUID().toString();
        Set<ItemOffer> cartItems = cloneItemsWith(generateSomeItems(5), offer -> {
            offer.setBundleId(someBundleId);
            offer.setBundlePromoId(md5Hex(someBundleId));
        }).collect(Collectors.toCollection(HashSet::new));

        cartItems.forEach(generated ->
                generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()));
        Multimap<String, CartItem> bundledItems = groupByBundles(cartItems);

        assertEquals(2, bundledItems.keySet().size());

        Collection<CartItem> itemsWithoutBundles = bundledItems.get(BUNDLE_NULL);

        assertTrue(CollectionUtils.isNotEmpty(itemsWithoutBundles));
        assertTrue(CollectionUtils.isNotEmpty(bundledItems.get(someBundleId)));

        storageCartService.deleteItems(owner, itemsWithoutBundles.stream()
                .map(CartItem::getId)
                .collect(Collectors.toList()));

        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));
        assertTrue(CollectionUtils.isNotEmpty(actual.getItems()));
        assertFalse(CollectionUtils.containsAny(actual.getItems(), itemsWithoutBundles));
        assertTrue(CollectionUtils.containsAll(actual.getItems(), bundledItems.get(someBundleId)));
    }

    @Test
    public void batchDeletingBundledItemsShouldNotAffectOtherItems() {
        String someBundleId = UUID.randomUUID().toString();
        String anotherBundle = UUID.randomUUID().toString();
        Set<ItemOffer> prototypeList = generateSomeItems(5).collect(Collectors.toSet());
        Set<ItemOffer> cartItems = Stream.concat(
                cloneItemsWith(prototypeList.stream(), offer -> {
                    offer.setBundleId(someBundleId);
                    offer.setBundlePromoId(md5Hex(someBundleId));
                }), cloneItemsWith(prototypeList.stream(), offer -> {
                    offer.setBundleId(anotherBundle);
                    offer.setBundlePromoId(md5Hex(offer.getBundleId()));
                })).collect(Collectors.toCollection(HashSet::new));

        cartItems.forEach(generated ->
                generated.setId(storageCartService.createOrReplaceItemByMsku(owner, generated).getResult()));

        Multimap<String, CartItem> bundledItems = groupByBundles(cartItems);

        assertEquals(3, bundledItems.keySet().size());

        Collection<CartItem> itemsToRemove = bundledItems.get(someBundleId);

        assertTrue(CollectionUtils.isNotEmpty(itemsToRemove));

        storageCartService.deleteItems(owner, itemsToRemove.stream()
                .map(CartItem::getId)
                .collect(Collectors.toList()));


        CartList actual = extractBasketList(storageCartService.getListsOwnerId(owner));

        assertTrue(CollectionUtils.isNotEmpty(cartItems));
        assertTrue(CollectionUtils.isNotEmpty(actual.getItems()));
        assertFalse(CollectionUtils.containsAny(actual.getItems(), itemsToRemove));
        assertTrue(CollectionUtils.containsAll(actual.getItems(), bundledItems.entries().stream()
                .filter(entry -> !someBundleId.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList())));
    }
}
