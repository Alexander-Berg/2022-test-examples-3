package ru.yandex.market.api.controller.sameshop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.RoomAddress;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.controller.v2.sameshop.SameShopController;
import ru.yandex.market.api.controller.v2.sameshop.request.SameShopControllerRequest;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.SameShopPackResult;
import ru.yandex.market.api.domain.v2.SameShopResult;
import ru.yandex.market.api.domain.v2.ThumbnailSize;
import ru.yandex.market.api.domain.v2.cart.CartItemV2;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.features.Feature;
import ru.yandex.market.api.internal.report.sameshop.request.ShoppingListItem;
import ru.yandex.market.api.internal.report.sameshop.request.ShoppingListItemType;
import ru.yandex.market.api.personal.Phone;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.user.order.Buyer;
import ru.yandex.market.api.user.order.OrderController;
import ru.yandex.market.api.user.order.Payload;
import ru.yandex.market.api.user.order.ShopOfferId;
import ru.yandex.market.api.user.order.ShopOrderItem;
import ru.yandex.market.api.user.order.checkout.AddressDeliveryPoint;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;
import ru.yandex.market.api.user.order.checkout.CheckoutResponse;
import ru.yandex.market.api.user.order.checkout.DeliveryPoint;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.CarterTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersonalTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("SameShopControllerTest")
public class SameShopControllerTest extends BaseTest {

    @Profile("SameShopControllerTest")
    @org.springframework.context.annotation.Configuration
    public static class Configuration {
        @Bean
        @Primary
        public CheckouterAPI checkouter() {
            return Mockito.mock(CheckouterAPI.class);
        }
    }

    private static final int SHOP_ID = 774;
    private static final int REGION_ID = 213;
    private static final long USER_ID = 123;
    private static final String RECIPIENT = "recipient";
    private static final String DELIVERY_POINT_PHONE = "911";
    private static final String ADDRESS_COUNTRY = "Россия";
    private static final String ADDRESS_CITY = "Москва";
    private static final String ADDRESS_HOUSE = "1";
    private static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(123.0);
    private static final int ITEM_COUNT = 1;
    private static final long FEED_ID = 567;
    private static final String UUID = "12345678901234567890123456789012";
    private static final Collection<String> TEST_RGBS = Collections.singletonList("white");

    @Inject
    private SameShopController sameShopController;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private OfferIdEncodingService offerIdEncodingService;

    @Inject
    private OrderController orderController;

    @Inject
    private CheckouterAPI checkouter;

    @Inject
    private CarterTestClient carterTestClient;

    @Inject
    private PersonalTestClient personalTestClient;

    /**
     * Получаем данные по покупке в одном магазине.
     * Ждем что сохраняем оригинальные значения wareMd5 (из выдачи репорта могут прийти новые wareMd5 отличающиеся от оригинальных)
     */
    @Test
    public void callSameShopRequest_waitOriginalWareMd5ValuesArePreservedInsideOfferId() {
        OfferId[] offerIds = new OfferId[]{
            new OfferId("wareMd5_1", "feeshow1"),
            new OfferId("wareMd5_2", "feeshow2")
        };

        reportTestClient.find(offerIds,
            "ss-test1-alt_same_shop.json");

        OfferId[] newOfferId = new OfferId[]{
            offerIdEncodingService.decode("newWareMd5_1"),
            offerIdEncodingService.decode("newWareMd5_2"),
        };
        reportTestClient.getOffersV2(newOfferId, "ss-test1-get_offers_v2.json");

        SameShopResult sameShopResult = callSameShop(getSameShopRequestBody(offerIds));

        Assert.assertEquals(1, sameShopResult.getSameShop().getOfferPacks().size());

        SameShopPackResult pack = sameShopResult.getSameShop().getOfferPacks().get(0);
        List<CartItemV2> offers = new ArrayList<>(pack.getItems());

        assertOfferWareMd5("wareMd5_1", "newWareMd5_1", offers.get(0).getOffer());
        assertOfferWareMd5("wareMd5_2", "newWareMd5_2", offers.get(1).getOffer());
    }

    /**
     * Используем offerId полученный из ручки покупки списком (с оригинальными wareMd5)
     * Пробуем оформить заказ, в случае успеха удаляем из корзины элементы с оригинальным wareMd5
     */
    @Test
    public void useOfferIdFromSameShop_checkout_waitItemsWithOriginalWareMd5WereDeletedFromCart() {
        OfferId offer1 = new OfferId("newWareMd5_1", "feeshow1", "wareMd5_1");
        OfferId offer2 = new OfferId("newWareMd5_2", "feeshow2", "wareMd5_2");
        ContextHolder.get().getFeatures().put(Feature.PERSONAL_ENABLED, "1");

        reportTestClient.getOffersV2(new OfferId[]{
                offer1,
                offer2,
        }, "ss-test2-get_offers.json");

        CheckoutRequest checkoutRequest = getCheckoutRequest(offer1, offer2);

        when(
            checkouter.checkout(
                any(MultiOrder.class),
                any(CheckoutParameters.class)
            )
        ).thenReturn(getCheckouterMultiOrderResponse(checkoutRequest));

        carterTestClient.getCart(USER_ID, "ss-test2-get_cart.json");

        carterTestClient.removeItem(USER_ID, "2533123");
        carterTestClient.removeItem(USER_ID, "2533124");

        HttpServletRequest servletRequest = MockRequestBuilder.start().header("X-Forwarded-For-Y", "1.2.3.4").build();

        personalTestClient.multiTypesStore(Collections.singletonList(new Phone("+79876543210")));

        CheckoutResponse checkoutResponse = orderController.checkout(
            checkoutRequest,
            servletRequest,
            new User(new OauthUser(USER_ID), null, new Uuid(UUID), null),
            true,
            genericParams,
            new ValidationErrors(),
            TEST_RGBS,
            Collections.emptyList(),
            true,
            IntLists.singleton(1),
            IntLists.singleton(2));

        Futures.waitAndGet(checkoutResponse.getDeleteItemsFuture());

        Assert.assertNotNull(checkoutResponse);
        List<OfferId> checkoutOrderIds = checkoutResponse.getShopShopOrders()
            .stream()
            .flatMap(x -> x.getItems().stream())
            .map(x -> offerIdEncodingService.decode(x.getMarketOfferId()))
            .collect(Collectors.toList());
        OfferId offerId1 = checkoutOrderIds.get(0);
        OfferId offerId2 = checkoutOrderIds.get(1);
        if (!offerId1.getWareMd5().equals("newWareMd5_1")) {
            offerId1 = checkoutOrderIds.get(1);
            offerId2 = checkoutOrderIds.get(0);
        }
        Assert.assertEquals("newWareMd5_1", offerId1.getWareMd5());
        Assert.assertEquals("wareMd5_1", offerId1.getOriginalWareMd5());
        Assert.assertEquals("newWareMd5_2", offerId2.getWareMd5());
        Assert.assertEquals("wareMd5_2", offerId2.getOriginalWareMd5());
    }

    @Test
    public void checkoutMissingOffer() {
        OfferId offer1 = new OfferId("newWareMd5_1", "feeshow1", "wareMd5_1");
        ContextHolder.get().getFeatures().put(Feature.PERSONAL_ENABLED, "1");

        reportTestClient.getOffersV2(new OfferId[]{
                offer1,
        }, "no-offers.json");

        reportTestClient.getOfferByShopOfferId(new ShopOfferId(1L, "UNKNOWN_OFFER_ID"), "no-offers.json");

        CheckoutRequest checkoutRequest = getCheckoutRequest(offer1);

        MultiOrder multiOrder = new MultiOrder();

        Order order = new Order();
        order.setId(123456L);
        order.setShopOrderId(String.valueOf(123456L));
        order.setShopId((long) SHOP_ID);
        order.setBuyerCurrency(ru.yandex.common.util.currency.Currency.RUR);
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferId("UNKNOWN_OFFER_ID");
        orderItem.setFeedId(1L);
        order.setItems(Collections.singleton(orderItem));
        multiOrder.setOrderFailures(Collections.singletonList(new OrderFailure(order, OrderFailure.Code.UNKNOWN_ERROR)));

        when(
            checkouter.checkout(
                any(MultiOrder.class),
                any(CheckoutParameters.class)
            )
        ).thenReturn(multiOrder);

        HttpServletRequest servletRequest = MockRequestBuilder.start().header("X-Forwarded-For-Y", "1.2.3.4").build();

        personalTestClient.multiTypesStore(Collections.singletonList(new Phone("+79876543210")));

        CheckoutResponse checkoutResponse = orderController.checkout(
            checkoutRequest,
            servletRequest,
            new User(new OauthUser(USER_ID), null, new Uuid(UUID), null),
            true,
            genericParams,
            new ValidationErrors(),
            TEST_RGBS,
                Collections.emptyList(),
                true,
            IntLists.singleton(1),
            IntLists.singleton(2));

        Assert.assertEquals(
            ShopOrderItem.Error.MISSING,
            checkoutResponse.getShopOrder(SHOP_ID).getItems().get(0).getErrors().iterator().next()
        );
    }

    @Test
    public void requestOrderOptions_waitPreserveOriginalWareMd5() {
        OfferId offer1 = new OfferId("newWareMd5_1", "feeshow1", "wareMd5_1");
        OfferId offer2 = new OfferId("newWareMd5_2", "feeshow2", "wareMd5_2");

        reportTestClient.getOffersV2(new OfferId[]{
                offer1,
                offer2,
        }, "ss-test2-get_offers.json")
                // 1) convertToMultiCard for checkouter
                // 2) one more time for checkouter converter
                .times(2);

        OrderOptionsRequest orderOptionRequest = getOrderOptionRequest(offer1, offer2);
        MultiOrder multiCart = getCheckouterOptionsResponse(orderOptionRequest);

        when(
            checkouter.cart(
                any(MultiOrder.class),
                any(CartParameters.class)
            )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = orderController.getOrderOptions(
            orderOptionRequest,
            new User(new OauthUser(USER_ID), null, new Uuid(UUID), null),
            null,
            true,
            false,
            false,
            new GenericParamsBuilder()
                .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                .build(),
            new ValidationErrors(),
            TEST_RGBS,
            false,
                false,
                Collections.emptyList(),
                true
        );

        Assert.assertEquals(1, orderOptions.getShops().size());

        OrderOptionsResponse.ShopOptions shopOptions = orderOptions.getShops().get(0);

        List<ShopOrderItem> items = shopOptions.getItems();
        Assert.assertEquals(2, items.size());

        OfferId optionOfferId1 = offerIdEncodingService.decode(items.get(0).getMarketOfferId());
        OfferId optionOfferId2 = offerIdEncodingService.decode(items.get(1).getMarketOfferId());
        Assert.assertThat(
            Arrays.asList(optionOfferId1, optionOfferId2),
            Matchers.containsInAnyOrder(offer1, offer2)
        );
    }

    private MultiOrder getCheckouterOptionsResponse(OrderOptionsRequest orderOptionRequest) {
        MultiOrder multiOrder = new MultiOrder();
        long checkouterOrderId = 123456L;

        List<Order> orders = new ArrayList<>();
        for (OrderOptionsRequest.ShopOrder o : orderOptionRequest.getShopOrders()) {
            Order order = new Order();

            order.setId(checkouterOrderId);
            order.setShopOrderId(String.valueOf(checkouterOrderId));
            ++checkouterOrderId;
            order.setShopId((long) SHOP_ID);

            order.setItems(o.getItems().stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    String wareMd5 = item.getOfferId().getWareMd5();
                    orderItem.setWareMd5(wareMd5);
                    orderItem.setOfferId(getShopOfferId(wareMd5));
                    orderItem.setShowInfo(item.getOfferId().getFeeShow());
                    orderItem.setFeedId(FEED_ID);
                    orderItem.setCount(item.getCount());
                    return orderItem;
                })
                .collect(Collectors.toList()));

            orders.add(order);
        }
        multiOrder.setOrders(orders);
        multiOrder.setBuyerRegionId(null);

        return multiOrder;
    }

    private OrderOptionsRequest getOrderOptionRequest(OfferId... offers) {
        OrderOptionsRequest request = new OrderOptionsRequest();
        request.setRegionId(REGION_ID);
        request.setCurrency(Currency.RUR);
        request.setShopOrders(getShopOrdersForOptions(offers));
        return request;
    }

    private void assertOfferWareMd5(String originalWareMd5, String newWareMd5, OfferV2 offer) {
        Assert.assertEquals(newWareMd5, offer.getWareMd5());
        Assert.assertEquals(originalWareMd5, offer.getId().getOriginalWareMd5());
        Assert.assertEquals(newWareMd5, offer.getId().getWareMd5());
        Assert.assertEquals(originalWareMd5, offer.getId().getOriginalWareMd5());
    }

    private SameShopResult callSameShop(SameShopControllerRequest body) {
        PageInfo pageInfo = new PageInfo(1, 10);
        Collection<Field> fields = new ArrayList<>();
        ValidationErrors validationErrors = new ValidationErrors();
        ApiDeferredResult<SameShopResult> packs = sameShopController.getPacks(
            false,
            pageInfo,
            new HashMap<>(),
            fields,
            body,
            genericParams,
            validationErrors);
        return packs
            .waitResult();
    }

    private RoomAddress getAddress() {
        RoomAddress address = new RoomAddress();
        address.setCountry(ADDRESS_COUNTRY);
        address.setCity(ADDRESS_CITY);
        address.setHouse(ADDRESS_HOUSE);
        return address;
    }

    private Buyer getBuyer() {
        Buyer buyer = new Buyer();
        buyer.setLastName("lastName");
        buyer.setFirstName("firstName");
        buyer.setEmail("hello@example.com");
        buyer.setPhone("+79876543210");
        return buyer;
    }

    @NotNull
    private CheckoutRequest getCheckoutRequest(OfferId... offers) {
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setRegionId(REGION_ID);
        checkoutRequest.setCurrency(Currency.RUR);
        checkoutRequest.setBuyer(getBuyer());
        checkoutRequest.setShopOrders(getShopOrders(offers));
        return checkoutRequest;
    }

    private MultiOrder getCheckouterMultiOrderResponse(CheckoutRequest checkoutRequest) {
        MultiOrder multiOrder = new MultiOrder();
        long checkouterOrderId = 123456L;

        List<Order> orders = new ArrayList<>();
        for (CheckoutRequest.ShopOrder o : checkoutRequest.getShopOrders()) {
            Order order = new Order();

            order.setId(checkouterOrderId);
            order.setShopOrderId(String.valueOf(checkouterOrderId));
            ++checkouterOrderId;
            order.setShopId((long) SHOP_ID);
            order.setBuyerCurrency(ru.yandex.common.util.currency.Currency.RUR);

            order.setItems(o.getItems().stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    String wareMd5 = item.getOfferId().getWareMd5();
                    orderItem.setWareMd5(wareMd5);
                    orderItem.setOfferId(getShopOfferId(wareMd5));
                    orderItem.setFeedId(FEED_ID);
                    return orderItem;
                })
                .collect(Collectors.toList()));

            orders.add(order);
        }
        multiOrder.setOrders(orders);

        return multiOrder;
    }

    private DeliveryPointId getDeliveryOptionId() {
        return new DeliveryPointId("hash", "deliveryOptionId");
    }

    private DeliveryPoint getDeliveryPoint() {
        AddressDeliveryPoint deliveryPoint = new AddressDeliveryPoint();
        deliveryPoint.setDeliveryOptionId(getDeliveryOptionId());
        deliveryPoint.setRegionId(REGION_ID);
        deliveryPoint.setRecipient(RECIPIENT);
        deliveryPoint.setPhone(DELIVERY_POINT_PHONE);
        deliveryPoint.setAddress(getAddress());
        return deliveryPoint;
    }

    private OrderOptionsRequest.AddressDeliveryPoint getOptionsDeliveryPoint() {
        OrderOptionsRequest.AddressDeliveryPoint deliveryPoint = new OrderOptionsRequest.AddressDeliveryPoint();
        deliveryPoint.setRegionId(REGION_ID);
        deliveryPoint.setCountry(ADDRESS_COUNTRY);
        deliveryPoint.setCity(ADDRESS_CITY);
        deliveryPoint.setHouse(ADDRESS_HOUSE);
        return deliveryPoint;
    }

    private Payload getPayload(String wareMd5) {
        Payload payload = new Payload();
        payload.setFee("fee");
        payload.setFeedId(FEED_ID);
        payload.setMarketOfferId(wareMd5);
        payload.setShopOfferId(getShopOfferId(wareMd5));
        return payload;
    }

    @NotNull
    private SameShopControllerRequest getSameShopRequestBody(OfferId[] ids) {
        SameShopControllerRequest body = new SameShopControllerRequest();
        List<ShoppingListItem> shoppingList = new ArrayList<>();
        Arrays.stream(ids).forEach(id -> shoppingList.add(new ShoppingListItem(ShoppingListItemType.OFFER, offerIdEncodingService.encode(id), 1, null)));
        body.setItems(shoppingList);
        return body;
    }

    @NotNull
    private String getShopOfferId(String wareMd5) {
        return "shopOfferId" + wareMd5;
    }

    @NotNull
    private List<CheckoutRequest.ShopOrder> getShopOrders(OfferId... offersIds) {
        List<CheckoutRequest.ShopOrder> shopOrders = new ArrayList<>();

        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequest.ShopOrder();

        List<CheckoutRequest.OrderItem> shopOrderItems = Arrays.stream(offersIds)
            .map(offerId -> {
                CheckoutRequest.OrderItem shopOrderItem = new CheckoutRequest.OrderItem();
                shopOrderItem.setOfferId(offerId);

                shopOrderItem.setPrice(ITEM_PRICE);
                shopOrderItem.setCount(ITEM_COUNT);
                shopOrderItem.setPayload(getPayload(offerId.getWareMd5()));
                return shopOrderItem;
            }).collect(Collectors.toList());
        shopOrder.setItems(shopOrderItems);

        shopOrder.setShopId(SHOP_ID);
        shopOrder.setDeliveryPoint(getDeliveryPoint());

        shopOrders.add(shopOrder);
        return shopOrders;
    }

    @NotNull
    private List<OrderOptionsRequest.ShopOrder> getShopOrdersForOptions(OfferId... offersIds) {
        List<OrderOptionsRequest.ShopOrder> shopOrders = new ArrayList<>();

        OrderOptionsRequest.ShopOrder shopOrder = new OrderOptionsRequest.ShopOrder();

        List<OrderOptionsRequest.OrderItem> shopOrderItems = Arrays.stream(offersIds)
            .map(offerId -> {
                OrderOptionsRequest.OrderItem shopOrderItem = new OrderOptionsRequest.OrderItem();
                shopOrderItem.setOfferId(offerId);

                shopOrderItem.setPrice(ITEM_PRICE);
                shopOrderItem.setCount(ITEM_COUNT);
                return shopOrderItem;
            }).collect(Collectors.toList());
        shopOrder.setItems(shopOrderItems);

        shopOrder.setShopId(SHOP_ID);
        shopOrder.setDeliveryPoint(getOptionsDeliveryPoint());

        shopOrders.add(shopOrder);
        return shopOrders;
    }
}
