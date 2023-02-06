package ru.yandex.market.checkout.carter.web;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.AddItemsRequest;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.ReplaceItemsRequest;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class CartResourceControllerDirectShopInShopTest extends CarterMockedDbTestBase {

    private static final String OFFER = "some_offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;

    private UserContext userContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
    }

    @AfterEach
    public void setDown() {
        reportMockConfigurer.resetMock();
    }

    @Test
    public void shouldSaveDirectShopInShopFlagOnItemCreation() {
        carterClient.addItem(AddItemRequest.builder()
                .withColor(Color.BLUE)
                .withUserIdType(userContext.getUserIdType())
                .withUserAnyId(userContext.getUserAnyId())
                .withCartItem(CartConverter.convert(generateItemWith(OFFER, offer -> offer.setDirectShopInShop(true))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).isDirectShopInShop(), is(true));
    }

    @Test
    public void shouldSaveDirectShopInShopFlagOnBatchItemCreation() {
        carterClient.addItems(AddItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setDirectShopInShop(true)))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).isDirectShopInShop(), is(true));
    }

    @Test
    public void shouldSaveDirectShopInShopFlagOnItemUpdating() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setDirectShopInShop(true)))))
                .build()));

        carterClient.updateItem(
                userContext.getUserAnyId(),
                userContext.getUserIdType(),
                Carter.DEFAULT_LIST_ID,
                Color.BLUE,
                itemOf(basket, keyOf(OFFER)).getId(),
                3,
                null
        );

        basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).isDirectShopInShop(), is(true));
    }

    @Test
    public void shouldSaveDirectShopInShopFlagOnBatchItemsUpdating() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setDirectShopInShop(true)))))
                .build()));

        carterClient.updateItems(
                userContext.getUserAnyId(),
                userContext.getUserIdType(),
                Color.BLUE,
                CartConverter.convert(createCartFor(userContext, itemOf(basket, keyOf(OFFER),
                        offer -> offer.setCount(3))))
        );

        basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).isDirectShopInShop(), is(true));
    }

    @Test
    public void shouldDisabledDirectShopInShopFlagOnDefaultItemCreation() {
        carterClient.addItem(AddItemRequest.builder()
                .withColor(Color.BLUE)
                .withUserIdType(userContext.getUserIdType())
                .withUserAnyId(userContext.getUserAnyId())
                .withCartItem(CartConverter.convert(generateItem(OFFER)))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).isDirectShopInShop(), is(false));
    }

}
