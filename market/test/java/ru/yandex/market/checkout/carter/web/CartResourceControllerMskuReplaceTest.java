package ru.yandex.market.checkout.carter.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.addItemTo;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_USER_GROUP;

public class CartResourceControllerMskuReplaceTest extends CarterMockedDbTestBase {

    private static final int LIST_ID = -1;

    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private Carter carterClient;

    private UserContext userContext;
    private CartList basket;

    @BeforeEach
    public void prepare() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));
    }

    @Test
    public void shouldNotReplaceItemsByMskuOnBatchAdd() {
        basket.addItem(generateItem("ssku1.2", 1, 1));
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        Map<Pair<String, String>, ItemOffer> changeSet = new HashMap<>();
        addItemTo(changeSet, generateItemWithBundle("ssku1.1", "some bundle", 1, 1));
        addItemTo(changeSet, generateItemWithBundle("ssku2.1", "some bundle", 1, 2));

        carterClient.addItems(
                userContext.getUserAnyId(), UID, LIST_ID, BLUE, CartConverter.convert(asCart(userContext, changeSet)),
                PARAM_USER_GROUP
        );

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basket, hasProperty("items", hasSize(3)));
        assertThat(basket, hasProperty("items", hasItems(
                hasProperty("objId", equalTo("ssku1.1")),
                hasProperty("objId", equalTo("ssku1.2")),
                hasProperty("objId", equalTo("ssku2.1"))
        )));

    }

    @Test
    public void shouldNotReplaceBundledItemsByMskuOnAdd() {
        basket.addItem(generateItemWithBundle("ssku1.1", "some bundle", 1, 1));
        basket.addItem(generateItemWithBundle("ssku2.1", "some bundle", 1, 2));
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        carterClient.addItem(
                userContext.getUserAnyId(), UID, LIST_ID, BLUE, CartConverter.convert(generateItem("ssku1.2", 1, 1)),
                PARAM_USER_GROUP);

        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        assertThat(basket, hasProperty("items", hasSize(3)));
        assertThat(basket, hasProperty("items", hasItems(
                hasProperty("objId", equalTo("ssku1.1")),
                hasProperty("objId", equalTo("ssku1.2")),
                hasProperty("objId", equalTo("ssku2.1"))
        )));

    }
}
