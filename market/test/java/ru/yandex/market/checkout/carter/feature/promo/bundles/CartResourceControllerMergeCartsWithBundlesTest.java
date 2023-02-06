package ru.yandex.market.checkout.carter.feature.promo.bundles;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.BundleKey;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.market.checkout.carter.model.Color.BLUE;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.model.UserIdType.UUID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateMskuItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class CartResourceControllerMergeCartsWithBundlesTest extends CarterMockedDbTestBase {

    private static final String FIRST_OFFER = "first offer";
    private static final String SECOND_OFFER = "second offer";
    private static final String THIRD_OFFER = "third offer";
    private static final String BUNDLE = "some bundle";
    private static final String ANOTHER_BUNDLE = "another bundle";

    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private Carter carterClient;

    private UserContext userContextFrom;
    private UserContext userContextTo;

    @BeforeEach
    public void prepare() {
        userContextFrom = UserContext.of(
                OwnerKey.of(BLUE, UUID, "" + Math.abs(ThreadLocalRandom.current().nextLong())));
        userContextTo = UserContext.of(
                OwnerKey.of(
                        BLUE, UID,
                        "" + Math.abs(ThreadLocalRandom.current().nextLong())));
    }

    @Test
    public void shouldNotFailOnEmptyCarts() {
        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));
    }

    @Test
    public void shouldNotFailOnEmptyProviderCart() {
        CartList basket = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 1),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                empty()
        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket, keyOf(FIRST_OFFER))),
                        is(itemOf(basket, keyOf(SECOND_OFFER))),
                        is(itemOf(basket, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldNotFailOnEmptyConsumerCart() {
        CartList basket = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 1),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket, keyOf(FIRST_OFFER))),
                        is(itemOf(basket, keyOf(SECOND_OFFER))),
                        is(itemOf(basket, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldMergeCarts() {
        userContextFrom = UserContext.of(
                OwnerKey.of(BLUE, UUID, "" + Math.abs(ThreadLocalRandom.current().nextLong())));
        userContextTo = UserContext.of(
                OwnerKey.of(BLUE, UID,
                        "" + Math.abs(ThreadLocalRandom.current().nextLong())));

        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 1),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(SECOND_OFFER, 2)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                empty()
        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket1, keyOf(FIRST_OFFER))),
                        is(itemOf(basket2, keyOf(SECOND_OFFER))),
                        is(itemOf(basket1, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldNotMergeConflictingItem() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 1),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 2)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        allOf(
                                is(itemOf(basket1, keyOf(FIRST_OFFER))),
                                hasProperty("id", is(itemOf(basket2, keyOf(FIRST_OFFER)).getId())),
                                hasProperty("count", is(3))
                        ),
                        is(itemOf(basket2, keyOf(SECOND_OFFER))),
                        is(itemOf(basket1, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldNotMergeConflictingItems() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 1),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 3)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        allOf(
                                is(itemOf(basket1, keyOf(FIRST_OFFER))),
                                hasProperty("id", is(itemOf(basket2, keyOf(FIRST_OFFER)).getId())),
                                hasProperty("count", is(3))
                        ),
                        allOf(
                                is(itemOf(basket1, keyOf(SECOND_OFFER))),
                                hasProperty("id", is(itemOf(basket2, keyOf(SECOND_OFFER)).getId())),
                                hasProperty("count", is(3))
                        ),
                        is(itemOf(basket1, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldNotMergeItemsIfAllPresented() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 4)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 1),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 3)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        allOf(
                                is(itemOf(basket1, keyOf(FIRST_OFFER))),
                                hasProperty("id", is(itemOf(basket2, keyOf(FIRST_OFFER)).getId())),
                                hasProperty("count", is(1))
                        ),
                        allOf(
                                is(itemOf(basket1, keyOf(SECOND_OFFER))),
                                hasProperty("id", is(itemOf(basket2, keyOf(SECOND_OFFER)).getId())),
                                hasProperty("count", is(2))
                        ),
                        is(itemOf(basket2, keyOf(THIRD_OFFER)))
                )
        );
    }

    @Test
    public void shouldAddBundledItemsToConsumer() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItemWithBundle(FIRST_OFFER, BUNDLE),
                generateItemWithBundle(THIRD_OFFER, BUNDLE)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 2)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket2, keyOf(FIRST_OFFER))),
                        is(itemOf(basket2, keyOf(SECOND_OFFER))),
                        is(itemOf(basket2, keyOf(THIRD_OFFER))),

                        is(itemOf(basket1, keyOf(FIRST_OFFER, BUNDLE))),
                        is(itemOf(basket1, keyOf(THIRD_OFFER, BUNDLE)))
                )
        );
    }

    @Test
    public void shouldSaveBundledItemsInConsumer() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 2)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItem(FIRST_OFFER, 3),
                generateItem(SECOND_OFFER, 2),
                generateItem(THIRD_OFFER, 2),

                generateItemWithBundle(FIRST_OFFER, BUNDLE),
                generateItemWithBundle(THIRD_OFFER, BUNDLE)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket1, keyOf(FIRST_OFFER))),
                        is(itemOf(basket1, keyOf(SECOND_OFFER))),
                        is(itemOf(basket1, keyOf(THIRD_OFFER))),

                        is(itemOf(basket2, keyOf(FIRST_OFFER, BUNDLE))),
                        is(itemOf(basket2, keyOf(THIRD_OFFER, BUNDLE)))
                )
        );
    }

    @Test
    public void shouldSkipSameBundledItems() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItemWithBundle(FIRST_OFFER, BUNDLE),
                generateItemWithBundle(THIRD_OFFER, BUNDLE)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItemWithBundle(FIRST_OFFER, ANOTHER_BUNDLE),
                generateItemWithBundle(THIRD_OFFER, ANOTHER_BUNDLE)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        not(itemOf(basket1, keyOf(FIRST_OFFER, BUNDLE))),
                        not(itemOf(basket1, keyOf(THIRD_OFFER, BUNDLE))),

                        is(itemOf(basket2, keyOf(FIRST_OFFER, ANOTHER_BUNDLE))),
                        is(itemOf(basket2, keyOf(THIRD_OFFER, ANOTHER_BUNDLE)))
                )
        );
    }

    @Test
    public void shouldMergeBundlesWithDifferentItems() {
        CartList basket1 = storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItemWithBundle(FIRST_OFFER, BUNDLE),
                generateItemWithBundle(SECOND_OFFER, BUNDLE)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItemWithBundle(FIRST_OFFER, ANOTHER_BUNDLE),
                generateItemWithBundle(THIRD_OFFER, ANOTHER_BUNDLE)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        is(itemOf(basket1, keyOf(FIRST_OFFER, BUNDLE))),
                        is(itemOf(basket1, keyOf(SECOND_OFFER, BUNDLE))),

                        is(itemOf(basket2, keyOf(FIRST_OFFER, ANOTHER_BUNDLE))),
                        is(itemOf(basket2, keyOf(THIRD_OFFER, ANOTHER_BUNDLE)))
                )
        );
    }

    @Test
    public void shouldMergeBundlesWithIdCollision() {
        storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateItemWithBundle(FIRST_OFFER, BUNDLE),
                generateItemWithBundle(THIRD_OFFER, BUNDLE),
                generateItemWithBundle(SECOND_OFFER, ANOTHER_BUNDLE),
                generateItemWithBundle(THIRD_OFFER, ANOTHER_BUNDLE)
        )).getResult();

        CartList basket2 = storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateItemWithBundle(FIRST_OFFER, ANOTHER_BUNDLE),
                generateItemWithBundle(THIRD_OFFER, ANOTHER_BUNDLE)
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        assertThat(
                CartConverter.convert(carterClient.getCart(
                        CartRequest.builder(
                                userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                                .withConsolidate(false)
                                .withRgb(BLUE)
                                .build())).getBasketList().getItems(),
                hasItems(
                        allOf(
                                hasProperty("name", is(SECOND_OFFER)),
                                hasProperty("bundleId", startsWith(BundleKey.UUID_PREFIX))
                        ),
                        allOf(
                                hasProperty("name", is(THIRD_OFFER)),
                                hasProperty("bundleId", startsWith(BundleKey.UUID_PREFIX))
                        ),

                        is(itemOf(basket2, keyOf(FIRST_OFFER, ANOTHER_BUNDLE))),
                        is(itemOf(basket2, keyOf(THIRD_OFFER, ANOTHER_BUNDLE)))
                )
        );
    }

    @Test
    public void shouldSkipBundlesWithEqualMsku() {
        storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                generateMskuItemWith(123L, offer -> offer.setBundleId(BUNDLE)),
                generateMskuItemWith(124L, offer -> offer.setBundleId(BUNDLE))
        )).getResult();

        storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                generateMskuItemWith(123L, offer -> offer.setBundleId(ANOTHER_BUNDLE)),
                generateMskuItemWith(124L, offer -> offer.setBundleId(ANOTHER_BUNDLE))
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor()
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        List<CartItem> resultItems = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(
                        userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                        .withConsolidate(false)
                        .withRgb(BLUE)
                        .build())).getBasketList().getItems();

        assertThat(resultItems, hasSize(2));

        assertThat(resultItems, hasItems(
                allOf(
                        hasProperty("msku", comparesEqualTo(123L)),
                        hasProperty("bundleId", equalTo(ANOTHER_BUNDLE))
                ),
                allOf(
                        hasProperty("msku", comparesEqualTo(124L)),
                        hasProperty("bundleId", equalTo(ANOTHER_BUNDLE))
                )
        ));
    }

    @Test
    public void shouldNotSkipBundlesWithEqualMskuForMultiOffers() {
        ItemOffer itemOffer1 = generateMskuItemWith(123L, offer -> offer.setBundleId(BUNDLE));
        ItemOffer itemOffer2 = generateMskuItemWith(124L, offer -> offer.setBundleId(BUNDLE));
        storageCartService.replaceCartListOwnerId(userContextFrom, createCartFor(
                userContextFrom,
                itemOffer1,
                itemOffer2
        )).getResult();

        ItemOffer itemOffer3 = generateMskuItemWith(123L, offer -> offer.setBundleId(ANOTHER_BUNDLE));
        ItemOffer itemOffer4 = generateMskuItemWith(124L, offer -> offer.setBundleId(ANOTHER_BUNDLE));
        storageCartService.replaceCartListOwnerId(userContextTo, createCartFor(
                userContextTo,
                itemOffer3,
                itemOffer4
        )).getResult();

        assertThat(carterClient.mergeItems(
                userContextFrom.getUserAnyId(),
                userContextFrom.getUserIdType(),
                userContextTo.getUserAnyId(),
                userContextTo.getUserIdType(),
                userContextTo.getColor(),
                true
        ), is(true));

//        assertThat(
//                carterClient.getCart(
//                        CartRequest.builder(
//                                userContextFrom.getUserAnyId(), userContextFrom.getUserIdType())
//                                .withConsolidate(false)
//                                .withRgb(BLUE)
//                                .withEnableMultiOffers(true)
//                                .build()).getBasketList().getItems(),
//                empty()
//        );

        List<CartItem> resultItems = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(
                        userContextTo.getUserAnyId(), userContextTo.getUserIdType())
                        .withConsolidate(false)
                        .withRgb(BLUE)
                        .withEnableMultiOffers(true)
                        .build())).getBasketList().getItems();

        assertThat(resultItems, hasSize(4));

        assertThat(resultItems, hasItems(
                allOf(
                        hasProperty("msku", comparesEqualTo(123L)),
                        hasProperty("bundleId", equalTo(BUNDLE)),
                        hasProperty("objId", equalTo(itemOffer1.getObjId()))
                ),
                allOf(
                        hasProperty("msku", comparesEqualTo(124L)),
                        hasProperty("bundleId", equalTo(BUNDLE)),
                        hasProperty("objId", equalTo(itemOffer2.getObjId()))
                ),
                allOf(
                        hasProperty("msku", comparesEqualTo(123L)),
                        hasProperty("bundleId", equalTo(ANOTHER_BUNDLE)),
                        hasProperty("objId", equalTo(itemOffer3.getObjId()))
                ),
                allOf(
                        hasProperty("msku", comparesEqualTo(124L)),
                        hasProperty("bundleId", equalTo(ANOTHER_BUNDLE)),
                        hasProperty("objId", equalTo(itemOffer4.getObjId()))
                )
        ));
    }
}
