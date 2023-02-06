package ru.yandex.market.checkout.carter.web;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.json.format.ItemField;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.asCart;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.assertItemsEqual;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneAsMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.findById;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.hashWritableParams;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;


@ExtendWith(SpringExtension.class)
public class BlueCartResourceControllerUpdateCartItemsFieldTest extends CarterMockedDbTestBase {

    @Autowired
    private Carter carterClient;

    @Autowired
    private StorageCartService storageCartService;
    private UserContext userContext;
    private CartList basket;

    public static Stream<Arguments> parameterizedTestData() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return Arrays.asList(
                param(ItemField.NAME, item -> item.setName(UUID.randomUUID().toString())),
                param(ItemField.Offer.DESCRIPTION, item -> item.setDesc(UUID.randomUUID().toString())),
                param(ItemField.Offer.FEE, item -> item.setFee(UUID.randomUUID().toString())),
                param(ItemField.Offer.HID, item -> item.setHid(rnd.nextLong(1, 10000))),
                param(ItemField.Offer.IS_EXPIRED, item -> item.setExpired(true)),
                param(ItemField.Offer.MODEL_ID, item -> item.setModelId(rnd.nextLong(1, 10000))),
                param(ItemField.Offer.MSKU, item -> item.setMsku(rnd.nextLong(1, 10000))),
                param(ItemField.Offer.PRICE, item -> item.setPrice(BigDecimal.valueOf(rnd.nextLong(1, 10000)))),
                param(ItemField.Offer.SHOP_ID, item -> item.setShopId(rnd.nextLong(1, 10000))),
                param(ItemField.Offer.OUTLET_ID, item -> item.setOutletId(UUID.randomUUID().toString())),
                param(ItemField.Offer.KIND_2_PARAMS, item -> item.setKind2Params("WTF"))
        ).stream().map(Arguments::of);
    }

    private static Object[] param(String name, Consumer<ItemOffer> decorator) {
        return new Object[]{name, decorator};
    }

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
                generateItemWithBundle("some flashdrife gift", "promo action for flashdrive",
                        rnd.nextInt(1, 10)
                ));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void methodUpdateShouldChangeCart(String fieldName, Consumer<ItemOffer> itemDecorator) {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(basket), true);

        Map<Pair<String, String>, ItemOffer> changeSet = cloneAsMap(basket);

        ItemOffer offer = changeSet.get(keyOf("some smartphone offer", "promo action with gift"));

        int hashBefore = hashWritableParams(offer);

        itemDecorator.accept(offer);

        Assertions.assertNotEquals(hashBefore, hashWritableParams(offer));

        basket = CartConverter.convert(carterClient.updateCart(
                userContext.getUserAnyId(), UserIdType.UID, Color.BLUE, CartConverter.convert(asCart(userContext,
                        changeSet))));

        ItemOffer changed = findById(basket, offer.getId());

        Assertions.assertNotNull(changed);
        Assertions.assertNotEquals(hashBefore, hashWritableParams(changed));
        assertItemsEqual(offer, changed);
    }
}
