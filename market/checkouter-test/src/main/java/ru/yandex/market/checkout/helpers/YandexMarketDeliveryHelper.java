package ru.yandex.market.checkout.helpers;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.DeliveryRoute;

import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

@WebTestHelper
public class YandexMarketDeliveryHelper {

    private final OrderCreateHelper createHelper;

    @Autowired
    public YandexMarketDeliveryHelper(OrderCreateHelper createHelper) {
        this.createHelper = createHelper;
    }

    public Order createMarDoOrder(Long deliveryServiceId) {
        return createMarDoOrder(deliveryServiceId, false);
    }

    public Order createMarDoOrder(Long deliveryServiceId, boolean isPartnerInterface) {
        return newMarDoOrderBuilder()
                .withDeliveryServiceId(deliveryServiceId)
                .withDeliveryType(DeliveryType.PICKUP)
                .withPartnerInterface(isPartnerInterface)
                .withColor(Color.BLUE)
                .build();
    }

    public MarDoOrderBuilder newMarDoOrderBuilder() {
        return new MarDoOrderBuilder(createHelper);
    }

    public static class MarDoOrderBuilder {

        private final OrderCreateHelper createHelper;
        private Long deliveryServiceId;
        private Instant supplierShipmentDateTime;
        private DeliveryType deliveryType = DeliveryType.PICKUP;
        private boolean isPartnerInterface = true;
        private Integer shipmentDay = 1;
        private Long shopId;
        private Color color = Color.BLUE;
        private Order order = OrderProvider.getBlueOrder();
        private DeliveryPartnerType deliveryPartnerType = YANDEX_MARKET;
        private Supplier<ActualDelivery> actualDeliveryProvider;
        private Duration packagingTime = Duration.ofHours(23);
        private ZonedDateTime shipmentDateTimeBySupplier;
        private ZonedDateTime receptionDateTimeByWarehouse;
        private boolean withCombinator = false;
        private Experiments experiments = Experiments.empty();

        private MarDoOrderBuilder(OrderCreateHelper createHelper) {
            this.createHelper = createHelper;
        }

        public MarDoOrderBuilder withDeliveryServiceId(Long deliveryServiceId) {
            this.deliveryServiceId = deliveryServiceId;
            return this;
        }

        public MarDoOrderBuilder withSupplierShipmentDateTime(Instant supplierShipmentDateTime) {
            this.supplierShipmentDateTime = supplierShipmentDateTime;
            return this;
        }

        public MarDoOrderBuilder withDeliveryType(DeliveryType deliveryType) {
            this.deliveryType = deliveryType;
            return this;
        }

        public MarDoOrderBuilder withPartnerInterface(boolean isPartnerInterface) {
            this.isPartnerInterface = isPartnerInterface;
            return this;
        }

        public MarDoOrderBuilder withShipmentDay(int shipmentDay) {
            this.shipmentDay = shipmentDay;
            return this;
        }

        public MarDoOrderBuilder withShopId(long shopId) {
            this.shopId = shopId;
            return this;
        }

        public MarDoOrderBuilder withColor(Color color) {
            this.color = color;
            return this;
        }

        public MarDoOrderBuilder withOrder(Order order) {
            this.order = order;
            return this;
        }

        public MarDoOrderBuilder withDeliveryPartnerType(DeliveryPartnerType deliveryPartnerType) {
            this.deliveryPartnerType = deliveryPartnerType;
            return this;
        }

        public MarDoOrderBuilder withActualDelivery(ActualDelivery actualDelivery) {
            this.actualDeliveryProvider = () -> actualDelivery;
            return this;
        }

        public MarDoOrderBuilder withPackagingTime(Duration packagingTime) {
            this.packagingTime = packagingTime;
            return this;
        }

        public MarDoOrderBuilder withShipmentDateTimeBySupplier(ZonedDateTime shipmentDateTimeBySupplier) {
            this.shipmentDateTimeBySupplier = shipmentDateTimeBySupplier;
            return this;
        }

        public MarDoOrderBuilder withReceptionDateTimeByWarehouse(ZonedDateTime receptionDateTimeByWarehouse) {
            this.receptionDateTimeByWarehouse = receptionDateTimeByWarehouse;
            return this;
        }

        public MarDoOrderBuilder withCombinator(boolean withCombinator) {
            this.withCombinator = withCombinator;
            return this;
        }

        public MarDoOrderBuilder withExperiments(Experiments experiments) {
            this.experiments = experiments;
            return this;
        }

        public Parameters buildParameters() {
            if (shopId != null) {
                order.setShopId(shopId);
            }
            Parameters parameters;
            if (color == Color.BLUE) {
                parameters = defaultBlueOrderParameters(order);
                parameters.setPaymentMethod(PaymentMethod.YANDEX);
                parameters.configureMultiCart(multiCart -> multiCart.getCarts().forEach(
                        o -> o.setDelivery(DeliveryProvider.getEmptyDelivery())
                ));
                parameters.setEmptyPushApiDeliveryResponse();
            } else {
                parameters = new Parameters(order);
            }
            if (Objects.nonNull(color)) {
                parameters.setColor(color);
            }
            if (isPartnerInterface) {
                parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
            } else {
                parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
            }
            parameters.configureMultiCart(multiCart -> {
                multiCart.setPaymentMethod(PaymentMethod.YANDEX);
                multiCart.setPaymentType(PaymentType.PREPAID);
            });

            parameters.setDeliveryType(deliveryType);
            parameters.setDeliveryPartnerType(deliveryPartnerType);
            parameters.setDeliveryServiceId(deliveryServiceId);

            if (deliveryType == DeliveryType.DELIVERY) {
                parameters.getReportParameters().setLocalDeliveryOptions(
                        parameters.getOrder().getItems().stream()
                                .collect(Collectors.toMap(OrderItem::getFeedOfferId,
                                        oi -> Collections.singletonList(
                                                LocalDeliveryOptionProvider.getMarDoLocalDeliveryOption(
                                                        deliveryServiceId,
                                                        shipmentDay,
                                                        supplierShipmentDateTime,
                                                        packagingTime)),
                                        (e1, e2) -> e1)));
            }

            if (deliveryType == DeliveryType.DELIVERY || deliveryType == DeliveryType.POST) {
                parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());
            }
            if (actualDeliveryProvider == null) {
                if (color == Color.BLUE) {
                    actualDeliveryProvider = () -> {
                        ActualDeliveryProvider.ActualDeliveryBuilder actualDeliveryBuilder = ActualDeliveryProvider
                                .builder()
                                .addPost(1);
                        if (deliveryServiceId != null) {
                            actualDeliveryBuilder
                                    .addPickup(deliveryServiceId, shipmentDay, supplierShipmentDateTime,
                                            packagingTime, shipmentDateTimeBySupplier, receptionDateTimeByWarehouse)
                                    .addDelivery(deliveryServiceId, shipmentDay, supplierShipmentDateTime,
                                            packagingTime, shipmentDateTimeBySupplier, receptionDateTimeByWarehouse);
                        }
                        return actualDeliveryBuilder.build();
                    };
                } else {
                    actualDeliveryProvider = () -> null;
                }
            }
            ActualDelivery actualDelivery = actualDeliveryProvider.get();
            parameters.getReportParameters().setActualDelivery(actualDelivery);

            if (withCombinator) {
                DeliveryRoute deliveryRoute = DeliveryRouteProvider.fromActualDelivery(actualDelivery, deliveryType);
                parameters.getReportParameters().setDeliveryRoute(deliveryRoute);
                DeliveryRouteProvider.cleanActualDelivery(actualDelivery);
                parameters.setMinifyOutlets(true);
            }

            if (experiments != null && experiments.isNotEmpty()) {
                parameters.setExperiments(experiments);
            }

            return parameters;
        }

        public Order build() {
            return createHelper.createOrder(buildParameters());
        }
    }
}
