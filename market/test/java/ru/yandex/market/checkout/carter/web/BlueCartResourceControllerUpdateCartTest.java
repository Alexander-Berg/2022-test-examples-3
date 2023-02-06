package ru.yandex.market.checkout.carter.web;

import java.math.RoundingMode;
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
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.addItemTo;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneAsMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneOffer;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.fromItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.groupByBundles;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class BlueCartResourceControllerUpdateCartTest extends CarterMockedDbTestBase {

    @Autowired
    private Carter carterClient;
    @Autowired
    private StorageCartService storageCartService;
    private UserContext userContext;
    private CartList basket;

    @BeforeEach
    public void prepare() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        basket.addItem(generateItem("some toy offer", rnd.nextInt(1, 10)));
        basket.addItem(generateItem("some food offer", rnd.nextInt(1, 10)));
        basket.addItem(generateItem("some software offer", rnd.nextInt(1, 10)));
        basket.addItem(generateItem("some strange offer", rnd.nextInt(1, 10)));
        basket.addItem(generateItem("some smartphone offer", rnd.nextInt(1, 10)));
        basket.addItem(generateItem("some flashdrive offer", rnd.nextInt(1, 10)));

        basket.addItem(generateItemWithBundle("some smartphone offer", "promo action with gift"));
        basket.addItem(generateItemWithBundle("some gift offer", "promo action with gift"));

        basket.addItem(
                generateItemWithBundle("some flashdrive offer", "promo action for flashdrive",
                        rnd.nextInt(1, 10)
                ));
        basket.addItem(
                generateItemWithBundle("another one flashdrive offer", "promo action for flashdrive",
                        rnd.nextInt(1, 10)
                ));
    }

    @Test
    public void methodUpdateShouldAddNewItemToCart() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        addItemTo(changeSet, generateItem("another strange offer", 3));
        addItemTo(
                changeSet,
                generateItemWithBundle("another smartphone offer", "promo action with gift", 2)
        );

        basket = asCart(userContext, changeSet);

        assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));
    }

    @Test
    public void methodUpdateShouldRemoveItemsFromCart() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        changeSet.remove(keyOf("some gift offer", "promo action with gift"));
        changeSet.remove(keyOf("some flashdrive offer"));

        basket = asCart(userContext, changeSet);

        assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));
    }

    @Test
    public void methodUpdateShouldChangeCountOfItemsSeparately() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        changeSet.get(keyOf("some smartphone offer", "promo action with gift")).setCount(50);
        changeSet.get(keyOf("some smartphone offer")).setCount(150);

        basket = asCart(userContext, changeSet);

        assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));
    }

    @Test
    public void methodUpdateShouldSupportBundlesSplitting() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        addItemTo(changeSet, fromItem(cloneOffer(changeSet.remove(
                keyOf("some smartphone offer", "promo action with gift"))), item -> {
            item.setBundleId(null);
            item.setBundlePromoId(null);
        }));

        addItemTo(changeSet, fromItem(cloneOffer(changeSet.remove(
                keyOf("some gift offer", "promo action with gift"))), item -> {
            item.setBundleId(null);
            item.setBundlePromoId(null);
        }));

        ItemOffer prototype = changeSet.remove(
                keyOf("some flashdrive offer", "promo action for flashdrive"));

        addItemTo(changeSet, fromItem(cloneOffer(prototype), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
            item.setCount(6);
        }));

        addItemTo(changeSet, fromItem(cloneOffer(prototype), item -> {
            item.setBundleId("second one action with gift");
            item.setBundlePromoId("second one action with gift");
            item.setCount(4);
        }));

        prototype = changeSet.remove(
                keyOf("another one flashdrive offer", "promo action for flashdrive"));

        addItemTo(changeSet, fromItem(cloneOffer(prototype), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
            item.setCount(6);
        }));

        addItemTo(changeSet, fromItem(cloneOffer(prototype), item -> {
            item.setBundleId("second one action with gift");
            item.setBundlePromoId("second one action with gift");
            item.setCount(4);
        }));

        basket = asCart(userContext, changeSet);

        basket = assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));

        Assertions.assertEquals(2, groupByBundles(basket.getItems()).get("another one action with gift").size());
        Assertions.assertEquals(2, groupByBundles(basket.getItems()).get("second one action with gift").size());
        Assertions.assertEquals(0, groupByBundles(basket.getItems()).get("promo action with gift").size());
        Assertions.assertEquals(0, groupByBundles(basket.getItems()).get("promo action for flashdrive").size());
    }

    @Test
    public void methodUpdateShouldSupportBundlesMergeInExternalAction() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("some smartphone offer", "promo action with gift")), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
        }));

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("some gift offer", "promo action with gift")), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
        }));

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("some flashdrive offer", "promo action for flashdrive")), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
        }));

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("another one flashdrive offer", "promo action for flashdrive")), item -> {
            item.setBundleId("another one action with gift");
            item.setBundlePromoId("another one action with gift");
        }));

        basket = asCart(userContext, changeSet);

        basket = assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));

        Assertions.assertEquals(4, groupByBundles(basket.getItems()).get("another one action with gift").size());
        Assertions.assertEquals(0, groupByBundles(basket.getItems()).get("promo action with gift").size());
        Assertions.assertEquals(0, groupByBundles(basket.getItems()).get("promo action for flashdrive").size());
    }

    @Test
    public void methodUpdateShouldSupportBundlesMergeInSameAction() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("some flashdrive offer", "promo action for flashdrive")), item -> {
            item.setBundleId("promo action with gift");
            item.setBundlePromoId("promo action with gift");
        }));

        addItemTo(changeSet, fromItem(changeSet.remove(
                keyOf("another one flashdrive offer", "promo action for flashdrive")), item -> {
            item.setBundleId("promo action with gift");
            item.setBundlePromoId("promo action with gift");
        }));

        basket = asCart(userContext, changeSet);

        basket = assertCart(basket, CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(basket))));

        Assertions.assertEquals(4, groupByBundles(basket.getItems()).get("promo action with gift").size());
        Assertions.assertEquals(0, groupByBundles(basket.getItems()).get("promo action for flashdrive").size());
    }

    private CartList assertCart(CartList expected, CartList actual) {
        Map<Pair<String, String>, ItemOffer> expectations = asMap(expected);
        Map<Pair<String, String>, ItemOffer> actualItem = asMap(actual);

        Assertions.assertTrue(CollectionUtils.containsAll(actualItem.keySet(), expectations.keySet()));

        expectations.forEach((key, item) -> assertItem(item, actualItem.get(key)));
        return actual;
    }

    private void assertItem(ItemOffer expected, ItemOffer actual) {
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(expected.getCount(), actual.getCount());
        Assertions.assertEquals(expected.getBundlePromoId(), actual.getBundlePromoId());
        Assertions.assertEquals(
                expected.getPrice().setScale(2, RoundingMode.FLOOR),
                actual.getPrice().setScale(2, RoundingMode.FLOOR)
        );
        Assertions.assertEquals(expected.getName(), actual.getName());
    }

}
