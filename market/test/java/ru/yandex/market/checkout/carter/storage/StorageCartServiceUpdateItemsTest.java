package ru.yandex.market.checkout.carter.storage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.utils.CarterTestUtils;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.carter.util.Utils.asItemOffers;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class StorageCartServiceUpdateItemsTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";
    private static final String THIRD_OFFER = "third offer";

    private static final BigDecimal PRICE = BigDecimal.TEN.setScale(2, RoundingMode.FLOOR);
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    @Autowired
    private StorageCartService cartService;
    @Autowired
    private YdbDao ydbDao;
    private UserContext userContext;
    private CartList basket;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(
                OwnerKey.of(
                        Color.BLUE, UserIdType.UID, "" + rnd.nextLong(1, Integer.MAX_VALUE)));

        basket = extractBasketList(cartService.getListsOwnerId(userContext));

        basket = cartService.replaceCartListOwnerId(userContext, createCartFor(
                userContext,
                generateItem(FIRST_OFFER, rnd.nextInt(1, 10)),
                generateItem(SECOND_OFFER, rnd.nextInt(1, 10)),
                generateItem(THIRD_OFFER, rnd.nextInt(1, 10))
        )).getResult();
    }

    @Test
    public void shouldNotUpdateValuableFieldsOnCountUpdate() {
        cartService.updateItemCountOwnerId(userContext,
                itemOf(basket, keyOf(FIRST_OFFER)).getId(), 12
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("count", is(12)));
    }

    @Test
    public void shouldNotDeleteItemsOnItemUpdateForActualization() {
        assertThat(cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setPrice(PRICE))
                ), Set.of()).getResult(), everyItem(isIn(asItemOffers(basket.getItems()))));

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("price", is(PRICE)));
    }

    @Test
    public void shouldNotDeleteItemsOnSeparateUpdatesForActualization() {
        assertThat(cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setPrice(PRICE)),
                        itemOf(basket, keyOf(THIRD_OFFER), item -> item.setPrice(PRICE))
                ), Set.of()).getResult(), everyItem(isIn(asItemOffers(basket.getItems()))));

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("price", is(PRICE)));
        assertThat(itemOf(resultBasket, keyOf(THIRD_OFFER)), hasProperty("price", is(PRICE)));
    }

    @Test
    public void shouldNotDeleteItemsOnItemRecurringUpdateForActualization() {
        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setPrice(PRICE))
                ), Set.of());

        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setPrice(PRICE))
                ), Set.of());

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), everyItem(isIn(basket.getItems())));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)),
                hasProperty("price", is(PRICE)));
    }

    @Test
    public void shouldReplaceItemForActualization() {
        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> {
                            item.setObjId("some new item");
                            item.setId(0);
                        })
                ), Set.of(
                        itemOf(basket, keyOf(FIRST_OFFER)).getId()
                )
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), hasSize(3));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)),
                hasProperty("objId", is("some new item")));
    }

    @Test
    public void shouldDoMultipleReplacingItemsForActualization() {
        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> {
                            item.setObjId("some new item");
                            item.setId(0);
                        }),
                        itemOf(basket, keyOf(THIRD_OFFER), item -> {
                            item.setObjId("another new item");
                            item.setId(0);
                        })
                ), Set.of(
                        itemOf(basket, keyOf(FIRST_OFFER)).getId(),
                        itemOf(basket, keyOf(THIRD_OFFER)).getId()
                )
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), hasSize(3));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)),
                hasProperty("objId", is("some new item")));
        assertThat(itemOf(resultBasket, keyOf(THIRD_OFFER)),
                hasProperty("objId", is("another new item")));
    }

    @Test
    public void shouldReplacePriceToNullForActualization() {
        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> {
                            item.setPrice(null);
                            item.setId(0);
                        })
                ), Set.of(
                        itemOf(basket, keyOf(FIRST_OFFER)).getId()
                )
        );

        basket = extractBasketList(cartService.getListsOwnerId(userContext));

        assertThat(basket.getItems(), hasSize(3));
        assertThat(itemOf(basket, keyOf(FIRST_OFFER)), hasProperty("price", nullValue()));
    }

    @Test
    public void shouldReplacePriceFromNullForActualization() {
        shouldReplacePriceToNullForActualization();

        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> {
                            item.setPrice(PRICE);
                            item.setId(0);
                        })
                ), Set.of(
                        itemOf(basket, keyOf(FIRST_OFFER)).getId()
                )
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), hasSize(3));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("price", is(PRICE)));
    }

    //
    @Test
    public void shouldUpdatePriceFromNullForActualization() {
        shouldReplacePriceToNullForActualization();

        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setPrice(PRICE))
                ),
                Set.of()
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), hasSize(3));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("price", is(PRICE)));
    }

    @Test
    public void shouldUpdateExpiredFlagForActualization() {
        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setExpired(true))
                ),
                Set.of()
        );

        CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

        CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
        assertThat(resultBasket.getItems(), hasSize(3));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("expired", is(true)));

        cartService.updateListForActualization(userContext,
                ydbDao.loadItemsWithUserAndColor(userContext),
                List.of(
                        itemOf(basket, keyOf(FIRST_OFFER), item -> item.setExpired(false))
                ),
                Set.of()
        );

        resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));
        assertThat(itemOf(resultBasket, keyOf(FIRST_OFFER)), hasProperty("expired", is(false)));
    }

    @Test
    public void shouldFailReplaceShopIdToNullForActualization() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateListForActualization(userContext,
                    ydbDao.loadItemsWithUserAndColor(userContext),
                    List.of(
                            itemOf(basket, keyOf(FIRST_OFFER), item -> {
                                item.setShopId(null);
                                item.setId(0);
                            })
                    ), Set.of(
                            itemOf(basket, keyOf(FIRST_OFFER)).getId()
                    )
            );

            CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

            CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
            assertThat(resultBasket.getItems(), hasSize(3));
        });
    }

    @Test
    public void shouldFailReplaceHIDToNullForActualization() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateListForActualization(userContext,
                    ydbDao.loadItemsWithUserAndColor(userContext),
                    List.of(
                            itemOf(basket, keyOf(FIRST_OFFER), item -> {
                                item.setHid(null);
                                item.setId(0);
                            })
                    ), Set.of(
                            itemOf(basket, keyOf(FIRST_OFFER)).getId()
                    )
            );

            CartList resultBasket = extractBasketList(cartService.getListsOwnerId(userContext));

            CarterTestUtils.valuableFieldsShouldNotBeNull(resultBasket);
            assertThat(resultBasket.getItems(), hasSize(3));
        });
    }
}
