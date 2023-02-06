package ru.yandex.market.checkout.carter.web;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

public class CartResourceControllerItemAppendSpecificTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private StorageCartService storageCartService;

    private UserContext uidContext;
    private CartList basket;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
        basket = storageCartService.replaceCartListOwnerId(uidContext,
                createCartFor(
                        uidContext,
                        generateItem(FIRST_OFFER, 3)
                )).getResult();

        assertThat(basket, notNullValue());
        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems(), everyItem(
                hasProperty("count", comparesEqualTo(3))
        ));
    }

    @Test
    public void shouldNotReduceItemCountOnRepeatedSingleAddition() {
        carterClient.addItem(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                Carter.DEFAULT_LIST_ID,
                Color.BLUE,
                CartConverter.convert(generateItem(FIRST_OFFER, 1)),
                null
        );

        CartList result = extractBasketList(storageCartService.getListsOwnerId(uidContext));

        assertThat(result, notNullValue());
        assertThat(result.getItems(), hasSize(1));
        assertThat(result.getItems(), hasItem(
                allOf(
                        hasProperty("name", is(FIRST_OFFER)),
                        hasProperty("count", comparesEqualTo(3))
                )
        ));
    }

    @Test
    public void shouldNotReduceItemCountOnRepeatedBatchAddition() {
        carterClient.addItems(
                uidContext.getUserAnyId(),
                uidContext.getUserIdType(),
                Carter.DEFAULT_LIST_ID,
                Color.BLUE,
                CartConverter.convert(createCartFor(
                        uidContext,
                        generateItem(FIRST_OFFER),
                        generateItem(SECOND_OFFER)
                )),
                null
        );

        CartList result = extractBasketList(storageCartService.getListsOwnerId(uidContext));

        assertThat(result, notNullValue());
        assertThat(result.getItems(), hasSize(2));
        assertThat(result.getItems(), hasItems(
                allOf(
                        hasProperty("name", is(FIRST_OFFER)),
                        hasProperty("count", comparesEqualTo(3))
                ),
                allOf(
                        hasProperty("name", is(SECOND_OFFER)),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));
    }
}
