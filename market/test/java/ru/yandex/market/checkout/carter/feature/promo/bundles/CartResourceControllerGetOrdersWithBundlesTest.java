package ru.yandex.market.checkout.carter.feature.promo.bundles;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;

public class CartResourceControllerGetOrdersWithBundlesTest extends CarterMockedDbTestBase {

    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private Carter carterClient;

    private UserContext userContext;
    private CartList basket;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID,
                "" + Math.abs(ThreadLocalRandom.current().nextLong()))
        );
        basket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        basket.addItem(generateItem("some food offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some toys offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some car offer", rnd.nextInt(1, 20)));
        basket.addItem(generateItem("some strange offer", rnd.nextInt(1, 20)));
    }

    @Test
    public void shouldReturnItemsInReverseOrder() {
        basket.addItem(generateItemWithBundle("some bundle offer", "some bundle"));
        basket.addItem(generateItemWithBundle("some gift offer", "some bundle"));

        //force create time
        basket.getItems().forEach(item -> {
            storageCartService.createOrReplaceItemByMsku(userContext, item);
        });

        basket = CartConverter.convert(
                carterClient.getCart(userContext.getUserAnyId(), UserIdType.UID, Color.BLUE).getLists().get(0)
        );

        assertThat(basket.getItems(), hasItems(
                generateItemWithBundle("some gift offer", "some bundle"),
                generateItemWithBundle("some bundle offer", "some bundle"),
                generateItem("some strange offer"),
                generateItem("some car offer"),
                generateItem("some toys offer"),
                generateItem("some food offer")
        ));
    }

    @Test
    public void shouldReturnItemsWithPrimaryKeyOrdering() {
        basket.addItem(generateItemWithBundle("some gift offer", "some bundle"));
        basket.addItem(generateItemWithBundle("some bundle offer", "some bundle", true));

        //force create time
        basket.getItems().forEach(item -> {
            storageCartService.createOrReplaceItemByMsku(userContext, item);
        });

        basket = CartConverter.convert(
                carterClient.getCart(userContext.getUserAnyId(), UserIdType.UID, Color.BLUE).getLists().get(0)
        );

        assertThat(basket.getItems(), hasItems(
                generateItemWithBundle("some gift offer", "some bundle"),
                generateItemWithBundle("some bundle offer", "some bundle"),
                generateItem("some strange offer"),
                generateItem("some car offer"),
                generateItem("some toys offer"),
                generateItem("some food offer")
        ));

        assertThat(basket.getItems(), hasItem(allOf(
                hasProperty("objId", equalTo("some_bundle_offer")),
                hasProperty("bundleId", equalTo("some bundle")),
                hasProperty("primaryInBundle", equalTo(true))
        )));
    }

    @Test
    public void shouldJoinBundleItemsInReverseOrder() {
        basket.addItem(generateItemWithBundle("some gift offer", "some bundle"));
        basket.addItem(generateItem("another offer"));
        basket.addItem(generateItemWithBundle("some bundle offer", "some bundle", true));

        //force create time
        basket.getItems().forEach(item -> {
            storageCartService.createOrReplaceItemByMsku(userContext, item);
        });

        basket = CartConverter.convert(
                carterClient.getCart(userContext.getUserAnyId(), UserIdType.UID, Color.BLUE).getLists().get(0)
        );

        assertThat(basket.getItems(), hasItems(
                generateItem("another offer"),
                generateItemWithBundle("some gift offer", "some bundle"),
                generateItemWithBundle("some bundle offer", "some bundle"),
                generateItem("some strange offer"),
                generateItem("some car offer"),
                generateItem("some toys offer"),
                generateItem("some food offer")
        ));
    }
}
