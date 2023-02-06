package ru.yandex.market.checkout.carter.web;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.json.format.ItemField;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.AddItemsRequest;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.ReplaceItemsRequest;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.createCartFor;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.itemOf;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.keyOf;

public class CartResourceControllerPriceDropTestTest extends CarterMockedDbTestBase {

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
    public void shouldSendToReportPriceDropItemsMskus() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build()));

        ItemOffer itemOffer = itemOf(basket, keyOf(OFFER));

        ReportGeneratorParameters generatorParameters = new ReportGeneratorParameters().addReportOffer(
                itemOffer.getMsku(),
                new ReportGeneratorParameters.ReportOffer(itemOffer.getObjId(), 146, true)
        );
        reportMockConfigurer.mockReportOk(generatorParameters);

        carterClient.getCart(CartRequest.builder(
                userContext.getUserAnyId(), userContext.getUserIdType()
        )
                .withRgb(Color.BLUE)
                .withConsolidate(true)
                .withRegion(123L)
                .build()).getLists();

    }

    @Test
    public void shouldSavePriceDropFlagOnItemCreation() {
        carterClient.addItem(AddItemRequest.builder()
                .withColor(Color.BLUE)
                .withUserIdType(userContext.getUserIdType())
                .withUserAnyId(userContext.getUserAnyId())
                .withCartItem(CartConverter.convert(generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true))))
                .build());

        Cart basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build()));

        assertThat(basket.getBasketList().getItems(), hasSize(1));
        assertThat(basket.getBasketList().getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldSavePriceDropFlagOnBatchItemCreation() {
        carterClient.addItems(AddItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldSavePriceDropFlagOnCartReplacing() {
        carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldSavePriceDropFlagOnCartRepeatableReplacing() {
        carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build());

        carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldChangePriceDropFlagOnCartReplacing() {
        carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build());

        carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(false)))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(false));
    }

    @Test
    public void shouldSavePriceDropFlagOnItemUpdating() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
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

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldSavePriceDropFlagOnBatchItemsUpdating() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
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

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(true));
    }

    @Test
    public void shouldChangePriceDropFlagOnBatchItemsUpdating() {
        CartList basket = CartConverter.convert(carterClient.updateCart(ReplaceItemsRequest.builder()
                .withColor(Color.BLUE)
                .withUserAnyId(userContext.getUserAnyId())
                .withUserIdType(userContext.getUserIdType())
                .withCartList(CartConverter.convert(createCartFor(userContext, generateItemWith(OFFER,
                        offer -> offer.setPricedropPromoEnabled(true)))))
                .build()));

        carterClient.updateItems(
                userContext.getUserAnyId(),
                userContext.getUserIdType(),
                Color.BLUE,
                CartConverter.convert(createCartFor(userContext, itemOf(basket, keyOf(OFFER), offer -> {
                    offer.setFieldsToChange(Set.of(ItemField.PRICE_DROP_PROMO_ENABLED));
                    offer.setPricedropPromoEnabled(false);
                })))
        );

        basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        assertThat(basket.getItems(), hasSize(1));
        assertThat(basket.getItems().iterator().next().isPricedropPromoEnabled(), is(false));
    }
}
