package ru.yandex.market.checkout.carter.web;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.ReplaceItemsRequest;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.carter.json.format.ItemField.BUNDLE_ID;
import static ru.yandex.market.checkout.carter.json.format.ItemField.BUNDLE_IS_PRIMARY;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.cloneAsMap;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.extractBasketList;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.fromItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemWithIdOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class BlueCartResourceControllerUpdateItemsTest extends CarterMockedDbTestBase {

    private static final String OFFER = "some_offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private StorageCartService storageCartService;
    private UserContext userContext;
    private CartList blueBasket;
    private ThreadLocalRandom rnd;

    @BeforeEach
    public void prepare() {
        rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
        blueBasket = createCartFor(
                userContext,
                generateItem("some_offer", rnd.nextInt(1, 10)),
                generateItem("another offer", rnd.nextInt(1, 10)),
                generateItem("another one offer", rnd.nextInt(1, 10))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void updateItemsMethodShouldChangeCountForAllItems(Color color) {
        // create blue item, should be able to update from any color
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel basket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                color,
                CartConverter.convert(
                        createCartFor(
                                userContext,
                                itemWithIdOf(blueBasket, keyOf("some_offer"), item -> item.setCount(3)),
                                itemWithIdOf(blueBasket, keyOf("another offer"), item -> item.setCount(4)),
                                itemWithIdOf(blueBasket, keyOf("another one offer"), item -> item.setCount(5))
                        )
                )
        );

        assertThat(basket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("count", is(4))
                ),
                allOf(
                        hasProperty("name", is("another one offer")),
                        hasProperty("count", is(5))
                )
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldReplaceItemsRegardlessOfColor(Color color) {
        // create blue item, should be able to update from any color
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel basket = carterClient.updateCart(
                ReplaceItemsRequest.builder()
                        .withUserAnyId(userContext.getUserAnyId())
                        .withUserIdType(userContext.getUserIdType())
                        .withColor(color)
                        .withCartList(CartConverter.convert(createCartFor(
                                userContext,
                                generateItemWithBundle("some_offer", "some bundle", 3),
                                generateItemWithBundle("another offer", "some bundle", 3),
                                itemOf(blueBasket, keyOf("another one offer"), item -> item.setCount(5))
                        )))
                        .build()
        );

        assertThat(basket.getItems(), hasSize(3));
        assertThat(basket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another one offer")),
                        hasProperty("count", is(5))
                )
        ));

        basket = carterClient.getCart(CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(color)
                .build()).getLists().get(0);

        assertThat(basket.getItems(), hasSize(3));
        assertThat(basket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another one offer")),
                        hasProperty("count", is(5))
                )
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLUE", "WHITE"})
    public void shouldDeleteItemOnCartReplacingRegardlessOfColor(Color color) {
        // create blue item, should be able to update from any color
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel basket = carterClient.updateCart(
                ReplaceItemsRequest.builder()
                        .withUserAnyId(userContext.getUserAnyId())
                        .withUserIdType(userContext.getUserIdType())
                        .withColor(color)
                        .withCartList(CartConverter.convert(createCartFor(
                                userContext,
                                itemOf(blueBasket, keyOf("some_offer")),
                                itemOf(blueBasket, keyOf("another offer"))

                        )))
                        .build()
        );

        assertThat(basket.getItems(), hasSize(2));
        assertThat(basket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer"))
                ),
                allOf(
                        hasProperty("name", is("another offer"))
                )
        ));

        basket = carterClient.getCart(CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                .withRgb(color)
                .build()).getLists().get(0);

        assertThat(basket.getItems(), hasSize(2));
        assertThat(basket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer"))
                ),
                allOf(
                        hasProperty("name", is("another offer"))
                )
        ));
    }

    @Test
    public void updateItemsMethodShouldChangeCountForSomeItems() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel updatedBasket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                Color.BLUE,
                CartConverter.convert(
                        createCartFor(
                                userContext,
                                itemWithIdOf(blueBasket, keyOf("some_offer"), item -> item.setCount(3)),
                                itemWithIdOf(blueBasket, keyOf("another one offer"), item -> item.setCount(5))
                        )
                )
        );

        assertThat(updatedBasket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("count", is(3))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("count", is(cloneAsMap(blueBasket).get(keyOf("another offer")).getCount()))
                ),
                allOf(
                        hasProperty("name", is("another one offer")),
                        hasProperty("count", is(5))
                )
        ));
    }

    @Test
    public void updateItemsMethodShouldNotChangeWithoutsFieldsToUpdate() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel updatedBasket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                Color.BLUE,
                CartConverter.convert(
                        createCartFor(
                                userContext,
                                itemWithIdOf(blueBasket, keyOf("some_offer"), item -> {
                                    item.setBundleId("some bundle");
                                    item.setPrimaryInBundle(true);
                                }),
                                itemWithIdOf(blueBasket, keyOf("another offer"), item -> item.setBundleId("some " +
                                        "bundle"))
                        )
                )
        );

        assertThat(updatedBasket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("bundleId", is(not("some bundle"))),
                        hasProperty("primaryInBundle", is(not(true)))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("bundleId", is(not("some bundle"))),
                        hasProperty("primaryInBundle", is(false))
                ),
                hasProperty("name", is("another one offer"))
        ));
    }

    @Test
    public void updateItemsMethodShouldChangeBundleProperties() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

        CartListViewModel updatedBasket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                Color.BLUE,
                CartConverter.convert(
                        createCartFor(
                                userContext,
                                itemWithIdOf(blueBasket, keyOf("some_offer"), item -> {
                                    item.setFieldsToChange(Set.of(BUNDLE_ID, BUNDLE_IS_PRIMARY));
                                    item.setBundleId("some bundle");
                                    item.setPrimaryInBundle(true);
                                }),
                                itemWithIdOf(blueBasket, keyOf("another offer"), item -> {
                                    item.setFieldsToChange(Set.of(BUNDLE_ID));
                                    item.setBundleId("some bundle");
                                })
                        )
                )
        );

        assertThat(updatedBasket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("bundleId", is("some bundle")),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("bundleId", is("some bundle")),
                        hasProperty("primaryInBundle", is(false))
                ),
                hasProperty("name", is("another one offer"))
        ));
    }

    @Test
    public void updateItemsMethodShouldCleanBundleProperties() {
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(
                userContext,
                itemOf(blueBasket, keyOf("some_offer"), item -> {
                    item.setBundleId("some bundle");
                    item.setPrimaryInBundle(true);
                }),
                itemOf(blueBasket, keyOf("another offer"), item -> item.setBundleId("some bundle"))
        )), true);

        blueBasket = extractBasketList(storageCartService.getListsOwnerId(userContext));

        CartListViewModel updatedBasket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                Color.BLUE,
                CartConverter.convert(
                        createCartFor(
                                userContext,
                                itemWithIdOf(blueBasket, keyOf("some_offer", "some bundle"), item -> {
                                    item.setFieldsToChange(Set.of(BUNDLE_ID, BUNDLE_IS_PRIMARY));
                                    item.setBundleId(null);
                                }),
                                itemWithIdOf(blueBasket, keyOf("another offer", "some bundle"), item -> {
                                    item.setFieldsToChange(Set.of(BUNDLE_ID, BUNDLE_IS_PRIMARY));
                                    item.setBundleId(null);
                                })
                        )
                )
        );

        assertThat(updatedBasket.getItems(), hasItems(
                allOf(
                        hasProperty("name", is("some_offer")),
                        hasProperty("bundleId", nullValue())
                ),
                allOf(
                        hasProperty("name", is("another offer")),
                        hasProperty("bundleId", nullValue())
                )
        ));
    }


    @Test
    public void updateItemsMethodShouldFailOnNegativeCount() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

            blueBasket = extractBasketList(storageCartService.getListsOwnerId(userContext));

            carterClient.updateItems(
                    userContext.getUserAnyId(),
                    UserIdType.UID,
                    Color.BLUE,
                    CartConverter.convert(
                            createCartFor(
                                    userContext,
                                    itemOf(blueBasket, keyOf("some_offer"), item -> item.setCount(-1))
                            )
                    )
            );
        });
    }

    @Test
    public void updateItemsMethodShouldFailOnNonexistentItem() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(blueBasket), true);

            blueBasket = extractBasketList(storageCartService.getListsOwnerId(userContext));

            carterClient.updateItems(
                    userContext.getUserAnyId(),
                    UserIdType.UID,
                    Color.BLUE,
                    CartConverter.convert(
                            createCartFor(
                                    userContext,
                                    generateItem("some_offer", rnd.nextInt(1, 10))
                            )
                    )
            );
        });
    }

    @Test
    @DisplayName("CartFee при добавлении в корзину не меняется при обновлении")
    public void updateItemsMethodShouldNotChangeCartFee() {
        var initialFee = "initial fee";
        var newFee = "new fee";
        var item = generateItem(OFFER);
        item.setFee(initialFee);
        storageCartService.bulkUpdateItemsOwnerId(userContext, List.of(createCartFor(userContext, item)), true);

        blueBasket = extractBasketList(storageCartService.getListsOwnerId(userContext));
        assertNotNull(blueBasket);
        var items = blueBasket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) items.iterator().next()).getCartFee(), is(initialFee));

        var changedItem = fromItem(item, i -> {
            i.setFee(newFee);
            i.setCartFee(initialFee);
        });
        var updatedBasket = carterClient.updateItems(
                userContext.getUserAnyId(),
                UserIdType.UID,
                Color.BLUE,
                CartConverter.convert(createCartFor(userContext, changedItem))
        );

        var updatedItems = updatedBasket.getItems();
        assertThat(updatedItems, hasSize(1));
        assertThat(((ItemOffer) items.iterator().next()).getCartFee(), is(initialFee));
    }
}
