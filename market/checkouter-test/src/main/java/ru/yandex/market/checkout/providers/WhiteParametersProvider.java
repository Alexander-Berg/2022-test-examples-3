package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.PickupOption;

import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.getDeliveryDates;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemBuilder;

public final class WhiteParametersProvider {

    public static final long WHITE_SHOP_ID = 991234L;
    public static final long WHITE_BUSINESS_ID = 98765L;

    private WhiteParametersProvider() {
    }

    public static Parameters defaultWhiteParameters() {
        return shopDeliveryOrder(OrderProvider.orderBuilder()
                .item(OrderItemProvider.getOrderItem())
                .build());
    }

    public static Parameters defaultWhiteParameters(Clock clock) {
        return shopDeliveryOrder(OrderProvider.orderBuilder()
                .item(OrderItemProvider.getOrderItem())
                .build(), clock);
    }

    /**
     * @deprecated use {@link #shopDeliveryOrder(ru.yandex.market.checkout.checkouter.order.Order)} instead
     */
    @Deprecated
    public static Parameters defaultWhiteParameters(Order order) {
        return shopDeliveryOrder(order);
    }

    public static Parameters simpleWhiteParameters() {
        Parameters parameters = applyTo(new Parameters());
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder().build()
        );
        return parameters;
    }

    @Nonnull
    public static Parameters shopDeliveryOrder(@Nonnull Order order) {
        return shopDeliveryOrder(order, TestableClock.getInstance());
    }

    @Nonnull
    public static Parameters shopDeliveryOrder(@Nonnull Order order, @Nonnull Clock clock) {
        order.setDelivery(DeliveryProvider.shopSelfDelivery()
                .dates(getDeliveryDates(LocalDate.ofInstant(clock.instant(), clock.getZone())))
                .build());
        return applyTo(new Parameters(MultiCartProvider.single(order)), clock);
    }

    @Nonnull
    public static Parameters shopPickupDeliveryOrder(@Nonnull Order order) {
        order.setDelivery(DeliveryProvider.shopSelfPickupDeliveryByMarketOutletId().build());
        return shopPickupDeliveryParameters(order);
    }

    @Nonnull
    public static Parameters shopPickupDeliveryParameters(@Nonnull Order order) {
        var params = applyTo(new Parameters(MultiCartProvider.single(order)));
        params.setDeliveryType(DeliveryType.PICKUP);
        params.setShopId(DeliveryProvider.OUTLET_SHOP_ID);
        params.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .buildResponse(DeliveryResponse::new));
        return params;
    }

    @Nonnull
    public static Parameters applyTo(@Nonnull Parameters parameters) {
        return applyTo(parameters, Clock.systemDefaultZone());
    }

    @Nonnull
    public static Parameters applyTo(@Nonnull Parameters parameters, @Nonnull Clock clock) {
        parameters.setColor(Color.WHITE);
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> {
            cart.setRgb(Color.WHITE);
            cart.getItems().forEach(oi -> {
                oi.setShopSku(null);
                oi.setMsku(null);
            });
        });
        parameters.setShopId(WHITE_SHOP_ID);
        parameters.setBusinessId(WHITE_BUSINESS_ID);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryPartnerType(SHOP);
        parameters.getOrder().getDelivery().setDeliveryPartnerType(SHOP);
        parameters.getOrder().setTotal(parameters.getOrder().getItems().stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO));
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(SHOP)
                                .buildActualDeliveryOption(clock)
                        ).build()
        );
        if (parameters.getReportParameters().getOffers() == null) {
            parameters.getReportParameters().setOffers(parameters.getOrder().getItems().stream()
                    .map(FoundOfferBuilder::createFrom)
                    .peek(b -> b.configure(whiteOffer()))
                    .map(FoundOfferBuilder::build)
                    .collect(Collectors.toUnmodifiableList()));
        }
        parameters.setPushApiDeliveryResponse(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                .partnerType(SHOP)
                .buildResponse(DeliveryResponse::new));
        return parameters;
    }

    public static Consumer<FoundOfferBuilder> whiteOffer() {
        return b -> b.atSupplierWarehouse(true)
                .color(ru.yandex.market.common.report.model.Color.WHITE)
                .supplierType(SupplierType.THIRD_PARTY)
                .shopId(WHITE_SHOP_ID)
                .isFulfillment(false)
                .deliveryPartnerType("SHOP")
                .warehouseId(null)
                .shopSku(null)
                .marketSku(null);
    }

    @Nonnull
    public static OrderProvider.OrderBuilder shopDeliveryOrder() {
        return OrderProvider.orderBuilder()
                .configure(OrderProvider::applyWhiteDbsDefaults)
                .someLabel()
                .shopId(WHITE_SHOP_ID)
                .paymentMethod(PaymentMethod.YANDEX);
    }

    @Nonnull
    public static OrderItemProvider.OrderItemBuilder dsbsOrderItem() {
        return orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .someDimensions()
                .supplierId(WHITE_SHOP_ID)
                .warehouseId(null)
                .shopSku(null)
                .marketSku(null);
    }

    public static Parameters digitalOrderPrameters() {
        OrderItem orderItem = OrderItemProvider.buildOrderItemDigital("1");

        Parameters parameters = new Parameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.getOrder().setItems(List.of(orderItem));
        parameters.setColor(Color.WHITE);
        parameters.setShopId(WHITE_SHOP_ID);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryPartnerType(SHOP);
        parameters.setDeliveryType(DeliveryType.DIGITAL);
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDigitalDeliveryResponse();
        parameters.getOrder().setDelivery(deliveryResponse);
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());
        parameters.setPushApiDeliveryResponse(deliveryResponse);
        return parameters;
    }

    public static Parameters dbsPickupOrderWithCombinatorParameters(Consumer<PickupOption> pickupOptionConfig) {
        var parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(SHOP);

        parameters.configureMultiCart(multiCart -> multiCart.getCarts().forEach(
                o -> o.setDelivery(DeliveryProvider.getEmptyDelivery())
        ));

        var actualDelivery = ActualDeliveryProvider.builder()
                .addPickupWithPartnerType(MOCK_DELIVERY_SERVICE_ID, DeliveryOptionPartnerType.REGULAR)
                .build();
        pickupOptionConfig.accept(actualDelivery.getResults().get(0).getPickup().get(0));
        parameters.getReportParameters().setActualDelivery(actualDelivery);

        DeliveryRoute deliveryRoute = DeliveryRouteProvider.fromActualDelivery(actualDelivery, DeliveryType.PICKUP);
        parameters.getReportParameters().setDeliveryRoute(deliveryRoute);
        parameters.setMinifyOutlets(true);

        return parameters;
    }
}
