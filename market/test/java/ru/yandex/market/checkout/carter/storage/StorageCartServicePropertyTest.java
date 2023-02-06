package ru.yandex.market.checkout.carter.storage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class StorageCartServicePropertyTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";

    @Autowired
    private StorageCartService cartService;

    private UserContext userContext;

    @BeforeEach
    public void prepare() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(BLUE, UID, "" + rnd.nextLong(1,
                Integer.MAX_VALUE)));
    }

    @Test
    public void shouldSaveRequestIdOnItemSave() {
        RequestContextHolder.createNewContext();
        cartService.bulkUpdateItemsOwnerId(userContext, Collections.singletonList(createCartFor(
                userContext, generateItem(FIRST_OFFER)
        )), true);

        assertThat(extractBasketList(cartService.getListsOwnerId(userContext)).getItems(), hasItem(
                hasProperty("createRequestId", notNullValue())
        ));
    }

    @Test
    public void shouldSaveUpdateTimeOnItemUpdate() {
        RequestContextHolder.createNewContext();
        cartService.bulkUpdateItemsOwnerId(userContext, Collections.singletonList(createCartFor(
                userContext, generateItem(FIRST_OFFER)
        )), true);

        CartList expected = extractBasketList(cartService.getListsOwnerId(userContext));

        CartList nlist = new CartList(expected);
        nlist.setItems(List.of(
                itemOf(expected, keyOf(FIRST_OFFER), offer -> offer.setMsku(123L))
        ));
        assertThat(cartService.bulkUpdateItemsOwnerId(userContext, nlist).getResult().getItems(), hasItem(
                hasProperty("updateTime", notNullValue())
        ));
    }
}
