package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static java.util.Collections.singletonList;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.SHIPMENT_DAY;
import static ru.yandex.market.checkout.test.providers.OrderProvider.BUSINESS_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.loyalty.api.model.delivery.DeliveryType.COURIER;

public final class BlueParametersProvider {

    public static final long DELIVERY_SERVICE_ID = 12345L;
    public static final long PICKUP_SERVICE_ID = 100501L;
    public static final long ANOTHER_PICKUP_SERVICE_ID = 100502L;

    private BlueParametersProvider() {
    }

    //FF
    public static Parameters defaultBlueOrderParameters() {
        return defaultBlueOrderParameters((Order) null);
    }

    //FF
    public static Parameters prepaidBlueOrderParameters() {
        return defaultBlueOrderParameters();
    }

    //FF
    public static Parameters defaultBlueOrderParametersWithItems(OrderItem... items) {
        return defaultBlueOrderParametersWithItems(Arrays.asList(items));
    }

    public static Parameters defaultBlueOrderParametersWithItems(Collection<OrderItem> items) {
        Order order = OrderProvider.getBlueOrder();
        order.setItems(items);
        return defaultBlueOrderParameters(order);
    }

    //FF
    public static Parameters postpaidBlueOrderParameters() {
        Parameters params = defaultBlueParametersWithDelivery(DELIVERY_SERVICE_ID);
        params.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        return params;
    }

    //FF
    public static Parameters postpaidBlueOrderParameters(Long deliveryServiceId) {
        Parameters params = defaultBlueParametersWithDelivery(deliveryServiceId);
        params.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        return params;
    }

    //FF
    public static Parameters defaultBlueOrderParameters(boolean freeDelivery) {
        Parameters parameters = defaultBlueOrderParameters((Order) null);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        FFDeliveryProvider.setFFDeliveryParameters(parameters, freeDelivery);
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> {
            cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        });
        parameters.setEmptyPushApiDeliveryResponse();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setFreeDelivery(freeDelivery);
        if (freeDelivery) {
            Arrays.stream(DeliveryType.values()).forEach(type ->
                    parameters.getLoyaltyParameters().addDeliveryDiscount(type, LoyaltyDiscount.builder()
                            .promoType(PromoType.LIMITED_FREE_DELIVERY_PROMO)
                            .discount(getDeliveryDiscountForFreeDelivery(type))
                            .build()));
        }
        return parameters;
    }

    public static Parameters blueOrderParametersWithCisItems(int itemCount, BigDecimal itemPrice) {
        var parameters = defaultBlueOrderParameters();
        parameters.getOrders().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(orderItem -> {
                    orderItem.setCargoTypes(Set.of(980));
                    orderItem.setCount(itemCount);
                    orderItem.setBuyerPrice(itemPrice);
                });

        return parameters;
    }

    private static BigDecimal getDeliveryDiscountForFreeDelivery(DeliveryType type) {
        switch (type) {
            case COURIER:
                return ActualDeliveryProvider.DELIVERY_PRICE;
            case PICKUP:
                return ActualDeliveryProvider.PICKUP_PRICE;
            case POST:
                return ActualDeliveryProvider.POST_PRICE;
            default:
                return BigDecimal.ZERO;
        }
    }

    //FF
    public static Parameters defaultBlueOrderParameters(Order order) {
        return defaultBlueOrderParameters(order, null);
    }

    //FF
    public static Parameters defaultBlueOrderParameters(Buyer buyer) {
        return defaultBlueOrderParameters((Order) null, buyer);
    }

    //FF
    public static Parameters defaultBlueOrderParameters(Order order, Buyer buyer) {
        return defaultBlueOrderParameters(() ->
                FulfilmentProvider.defaultFulfilmentParameters(order, buyer));

    }

    //FF
    public static Parameters defaultBlueOrderParameters(MultiCart multiCart, Buyer buyer) {
        return defaultBlueOrderParameters(() -> FulfilmentProvider.defaultFulfilmentParameters(multiCart, buyer));
    }

    //FF
    public static Parameters defaultBlueOrderParameters(Supplier<Parameters> parametersSupplier) {
        Parameters parameters = parametersSupplier.get();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setColor(Color.BLUE);
        parameters.getOrders().forEach(item -> item.setRgb(BLUE)); // ensure all orders are blue - default can be green
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        //TODO: убрать в DeliveryProvider
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(PICKUP_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addDelivery(DELIVERY_SERVICE_ID)
                        .addPost(7)
                        .build()
        );
        reportParameters.setDeliveryVat(VatType.VAT_20);
        parameters.getReportParameters().setIgnoreStocks(false);
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> {
            Delivery delivery = DeliveryProvider.getEmptyDeliveryWithAddress();
            delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            cart.setDelivery(delivery);
        });
        parameters.setEmptyPushApiDeliveryResponse();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        return parameters;
    }

    //FBS
    public static Parameters defaultBlueNonFulfilmentOrderParameters() {
        return defaultBlueNonFulfilmentOrderParameters((Order) null);
    }

    //C&C
    public static Parameters clickAndCollectOrderParameters() {
        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .build());
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());
        return parameters;
    }

    //FBS
    public static Parameters defaultBlueNonFulfilmentOrderParameters(OrderItem... items) {
        Order order = OrderProvider.getBlueOrder();
        order.setItems(Arrays.asList(items));
        return defaultBlueNonFulfilmentOrderParameters(order);
    }

    //FBS
    public static Parameters defaultBlueNonFulfilmentOrderParameters(Order order) {
        Parameters parameters = new Parameters(order);
        parameters.getReportParameters()
                .setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.name()));
        parameters.setupFulfilmentData();
        parameters.getOrder().getItems()
                .forEach(oi -> parameters.getReportParameters()
                        .overrideItemInfo(oi.getFeedOfferId()).getFulfilment().fulfilment = false);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        return parameters;
    }

    //FF
    public static Parameters defaultBlueParametersWithDelivery(Long deliveryServiceId) {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setColor(Color.BLUE);
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        reportParameters.setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(PICKUP_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addPickup(ANOTHER_PICKUP_SERVICE_ID, 3, Collections.singletonList(321321321L))
                        .addDelivery(deliveryServiceId, 3)
                        .addPost(7)
                        .build()
        );
        reportParameters.setDeliveryVat(VatType.VAT_20);
        reportParameters.setIgnoreStocks(false);
        parameters.getBuiltMultiCart().getCarts().forEach(cart -> {
            cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        });
        parameters.setEmptyPushApiDeliveryResponse();
        return parameters;
    }

    //FF
    public static Parameters bluePrepaidWithCustomPrice(BigDecimal price) {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.setColor(Color.BLUE);
        parameters.getOrder().getItems().forEach(item -> {
            item.setBuyerPrice(price);
            item.setQuantPrice(price);
            item.setPrice(price);
        });
        return parameters;
    }

    //FF
    public static Parameters blueOrderWithDeliveryPromoParameters() {
        final Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setColor(BLUE);
        parameters.setShopId(SHOP_ID_WITH_SORTING_CENTER);
        parameters.setBusinessId(BUSINESS_ID);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setDeliveryType(ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder().addDelivery(MOCK_DELIVERY_SERVICE_ID, SHIPMENT_DAY).build()
        );
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().addDeliveryDiscount(COURIER,
                new LoyaltyDiscount(new BigDecimal(Integer.MAX_VALUE), PromoType.YANDEX_EMPLOYEE)
        );
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setDeliveryServiceId(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        parameters.setEmptyPushApiDeliveryResponse();
        //настраиваем бесплатную доставку по yandexemployee чтобы сходить в обход кеша
        parameters.configureMultiCart(multiCart -> multiCart.getCarts()
                .forEach(cart -> {
                    cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
                    cart.getDelivery().setBuyerAddress(AddressProvider.getAddress(
                            a -> a.setStreet(a.getStreet() + UUID.randomUUID().toString())));
                }));
        //координаты, по которым бесплатная доставка
        parameters.getGeocoderParameters().setGps("60.590898 56.835586");
        parameters.setYandexEmployee(true);

        return parameters;
    }

    //Express
    public static Parameters blueNonFulfilmentOrderWithExpressDelivery() {
        Parameters blueParams = BlueParametersProvider.defaultBlueOrderParameters();
        blueParams.getOrder().getItems().forEach(item -> {
            ItemInfo itemInfo = blueParams.getReportParameters().overrideItemInfo(item.getFeedOfferId());
            itemInfo.setAtSupplierWarehouse(true);
            itemInfo.setFulfilment(new ItemInfo.Fulfilment(blueParams.getShopId(), FulfilmentProvider.TEST_SKU,
                    FulfilmentProvider.TEST_SHOP_SKU, null, false));
        });

        blueParams.setDeliveryType(DELIVERY);
        for (ActualDeliveryResult actualDelivery : blueParams.getReportParameters().getActualDelivery().getResults()) {
            for (ActualDeliveryOption option : actualDelivery.getDelivery()) {
                option.setIsExpress(true);
                option.setDeliveryServiceId(1006360L);
            }
        }
        blueParams.getReportParameters().setIsExpress(true);
        return blueParams;
    }

    public static Parameters blueNonFulfilmentOrderWithExpressPickupDelivery() {
        Parameters blueParams = BlueParametersProvider.defaultBlueOrderParameters();
        blueParams.getOrder().getItems().forEach(item -> {
            ItemInfo itemInfo = blueParams.getReportParameters().overrideItemInfo(item.getFeedOfferId());
            itemInfo.setAtSupplierWarehouse(true);
            itemInfo.setFulfilment(new ItemInfo.Fulfilment(blueParams.getShopId(), FulfilmentProvider.TEST_SKU,
                    FulfilmentProvider.TEST_SHOP_SKU, null, false));
        });

        blueParams.setDeliveryType(PICKUP);

        // We have express courier options aswell as express to pickup option
        for (ActualDeliveryResult actualDelivery : blueParams.getReportParameters().getActualDelivery().getResults()) {
            for (PickupOption option : actualDelivery.getPickup()) {
                option.setIsExpress(true);
            }
            for (ActualDeliveryOption option : actualDelivery.getDelivery()) {
                option.setIsExpress(true);
                option.setDeliveryServiceId(1006360L);
            }
        }
        blueParams.getReportParameters().setIsExpress(true);
        return blueParams;
    }

    //FF
    public static Parameters blueOrderWithDeferredCourierDelivery() {
        return blueOrderWithDeferredCourierDelivery(13, 14);
    }

    //FF
    public static Parameters blueOrderWithDeferredCourierDelivery(int hourFrom, int hourTo) {
        Parameters blueParams = BlueParametersProvider.defaultBlueOrderParameters();
        blueParams.setDeliveryType(DELIVERY);
        for (ActualDeliveryResult actualDelivery : blueParams.getReportParameters().getActualDelivery().getResults()) {
            for (ActualDeliveryOption option : actualDelivery.getDelivery()) {
                option.setIsDeferredCourier(true);
                option.setDayFrom(1);
                option.setTimeIntervals(singletonList(new DeliveryTimeInterval(
                        LocalTime.of(hourFrom, 0),
                        LocalTime.of(hourTo, 0))));
            }
        }
        return blueParams;
    }
}
