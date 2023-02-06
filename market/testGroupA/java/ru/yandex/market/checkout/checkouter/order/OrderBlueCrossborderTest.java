package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper.CROSSBORDER_SUPPLIER_ID;

@Disabled("broken")
public class OrderBlueCrossborderTest extends AbstractWebTestBase {

    public static final String TRACK_CODE2 = "anotherTrack";
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BlueCrossborderOrderHelper blueCrossborderOrderHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @Tag(Tags.CROSSBORDER)
    @Test
    public void checkoutBlueCrossborderOrder() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildPostDeliveryResponse();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));
        Parameters parameters = blueCrossborderOrderHelper.setupParameters(deliveryResponse, new Parameters());
        MultiOrder multiOrder = blueCrossborderOrderHelper.checkoutCrossborder(parameters);
        multiOrder.getOrders().forEach(order -> Assertions.assertEquals(PaymentMethod.YANDEX,
                order.getPaymentMethod()));
        multiOrder.getOrders().forEach(order -> Assertions.assertEquals(Color.BLUE, order.getRgb()));
        assertThat(stockStorageConfigurer.getServeEvents(), Matchers.empty());
        multiOrder.getOrders().forEach(order ->
                order.getItems().forEach(i ->
                        Assertions.assertEquals((Long) CROSSBORDER_SUPPLIER_ID, i.getSupplierId())
                )
        );
        final List<String> pushApiRequestBodies = pushApiMock.getAllServeEvents().stream()
                .filter(serveEvent -> serveEvent.getRequest().getUrl().matches("/shops/\\d+/cart.*"))
                .map(se -> se.getRequest().getBodyAsString())
                .collect(Collectors.toList());
        assertThat(pushApiRequestBodies, hasSize(greaterThanOrEqualTo(1)));
        pushApiRequestBodies.forEach(
                body -> assertThat(body, Matchers.containsString("crossborder=\"true\""))
        );

    }

    @Tag(Tags.CROSSBORDER)
    @Test
    public void shouldNotCreateCrossborderPostpaidOrder() {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        Parameters parameters = blueCrossborderOrderHelper.setupParameters(deliveryResponse, new Parameters());
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), List.of(deliveryResponse), true);

        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        final Set<CartChange> changes = cart.getCarts().get(0).getChanges();
        Assertions.assertEquals(1, changes.size());
        assertThat(changes, Matchers.contains(CartChange.PAYMENT));
    }

    @Tag(Tags.CROSSBORDER)
    @Test
    public void checkoutDefaultBlueAndCrossborderMultiOrder() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildPostDeliveryResponse();
        deliveryResponse.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        Parameters crossborderParameters = blueCrossborderOrderHelper.setupParametersForMultiOrder(deliveryResponse);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addOrder(crossborderParameters);

        MultiCart cart = doCartBlueWithoutFulfilment(parameters);
        pushApiConfigurer.mockAccept(parameters.getOrders(), true);
        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                CheckoutParameters.builder()
                        .withUid(cart.getBuyer().getUid())
                        .withSandbox(false)
                        .withRgb(Color.BLUE)
                        .withContext(Context.MARKET)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withHitRateGroup(HitRateGroup.LIMIT)
                        .build()
        );

        assertThat(multiOrder.getOrderFailures(), anyOf(nullValue(), empty()));
        assertThat(multiOrder.getOrders(), hasSize(2));
        assertThat(
                multiOrder.getOrders().stream().map(Order::isFulfilment).collect(Collectors.toList()),
                containsInAnyOrder(true, false)
        );
        multiOrder.getOrders().forEach(order -> Assertions.assertEquals(PaymentMethod.YANDEX,
                order.getPaymentMethod()));

        orderPayHelper.payForOrders(multiOrder.getOrders());
    }

    private MultiCart doCartBlueWithoutFulfilment(Parameters parameters) throws IOException {
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiCart multiCartRequest = parameters.getBuiltMultiCart();
        multiCartRequest.setPaymentMethod(PaymentMethod.YANDEX);
        multiCartRequest.setPaymentType(PaymentType.PREPAID);
        return client.cart(multiCartRequest, cartParameters);
    }

    @Test
    @DisplayName("Проверяем, что под ролью магазина можно создавать парцел и трэк-коды")
    @Tag(Tags.CROSSBORDER)
    public void canAddTrackWithShopRole() throws Exception {
        final List<OrderItem> orderItems = Arrays.asList(
                OrderItemProvider.getOrderItem(),
                OrderItemProvider.getAnotherOrderItem()
        );
        final MultiOrder multiOrder = blueCrossborderOrderHelper
                .checkoutCrossborder(blueCrossborderOrderHelper.setupParameters(
                        new Parameters(OrderProvider.getBlueOrder(order -> order.setItems(orderItems)))));
        assertTrue(multiOrder.isValid());
        assertThat(multiOrder.getOrders(), hasSize(1));
        Order order = multiOrder.getOrders().iterator().next();
        assertThat(order.hasProperty(OrderPropertyType.IS_CROSSBORDER), is(true));
        assertThat(order.getItems(), hasSize(2));
        Order deliveryOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(deliveryOrder.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(deliveryOrder.getDelivery().getDeliveryPartnerType(), is(DeliveryPartnerType.SHOP));

        // нужно сначала создать парцел, в который войдёт первый товар заказа.
        final OrderItem[] deliveryOrderItems = deliveryOrder.getItems().toArray(new OrderItem[2]);

        final Parcel parcel = ParcelProvider
                .createParcel(
                        ParcelItemProvider.buildParcelItem(
                                deliveryOrderItems[0].getId(),
                                deliveryOrderItems[0].getCount()
                        )
                );
        final ClientInfo shopClientInfo = new ClientInfo(ClientRole.SHOP, order.getShopId());
        final Order orderWithParcel = orderDeliveryHelper.updateOrderDelivery(
                deliveryOrder.getId(),
                shopClientInfo,
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(parcel)
        );
        assertThat(orderWithParcel.getDelivery().getParcels(), hasSize(1));

        final Parcel originalParcel = orderWithParcel.getDelivery().getParcels().iterator().next();

        final Track track = TrackProvider.createTrack();

        // затем к этому парцелу цепляем трек код.
        orderDeliveryHelper.addTrack(
                orderWithParcel.getId(),
                originalParcel.getId(),
                track,
                shopClientInfo
        );
        final Order orderWithTrack = orderService.getOrder(deliveryOrder.getId());
        List<Track> tracks = orderWithTrack.getDelivery().getParcels().iterator().next().getTracks();
        assertThat(tracks, hasSize(1));
        assertThat(tracks.get(0).getTrackCode(), equalTo(track.getTrackCode()));

        // и ещё один парцел
        final Order orderWithTwoParcels = orderDeliveryHelper.updateOrderDelivery(
                deliveryOrder.getId(),
                shopClientInfo,
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                        orderWithTrack.getDelivery().getParcels().get(0),
                        ParcelProvider
                                .createParcel(
                                        ParcelItemProvider.buildParcelItem(
                                                deliveryOrderItems[1].getId(),
                                                deliveryOrderItems[1].getCount()
                                        )
                                )
                )
        );
        assertThat(orderWithTwoParcels.getDelivery().getParcels(), hasSize(2));

        final Parcel secondParcel = orderWithTwoParcels.getDelivery().getParcels().stream()
                .filter(p -> !p.getId().equals(originalParcel.getId()))
                .findFirst()
                .orElse(null);

        assertThat(secondParcel, notNullValue());

        final Track secondTrack = TrackProvider.createTrack(TRACK_CODE2, TrackProvider.DELIVERY_SERVICE_ID);


        // затем ко второму этому парцелу цепляем трек код.
        orderDeliveryHelper.addTrack(
                orderWithParcel.getId(),
                secondParcel.getId(),
                secondTrack,
                shopClientInfo
        );
        final Order orderWithTracks = orderService.getOrder(deliveryOrder.getId());
        tracks = orderWithTracks.getDelivery().getParcels().stream()
                .flatMap(p -> p.getTracks().stream())
                .collect(Collectors.toList());

        assertThat(tracks, hasSize(2));
        assertThat(tracks.stream()
                        .map(Track::getTrackCode)
                        .collect(Collectors.toList()),
                containsInAnyOrder(TrackProvider.TRACK_CODE, TRACK_CODE2)
        );

    }

}
