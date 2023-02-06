package ru.yandex.market.checkout.checkouter.order;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.checkout.AbstractPushApiTestBase;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.status.actions.UpdateShipmentStatusAction;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.util.CheckoutRequestUtils;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.SupplierProcessing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.yandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.offerOf;

public class ShipmentUpdateOnProcessingStatusForDropshipTest extends AbstractPushApiTestBase {

    private final Instant supplierShipmentDateTime = Instant.parse("2019-10-14T13:40:00Z");
    @Autowired
    private UpdateShipmentStatusAction action;
    @Autowired
    private OrderPayHelper orderPayHelper;
    private Order order;

    @BeforeEach
    public void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHIPMENT_DATE_BY_SUPPLIER_ENABLED, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHIPMENT_UPDATE_TO_ALL_ORDERS_ENABLED, true);
        setAsyncPushApi(true);
        setAsyncFBSPushApi(true);

        freezeTime();
        action.setClock(getClock());

        var item = orderItemWithSortingCenter().offer("some offer");

        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .deliveryBuilder(yandexDelivery()
                        .serviceId(MOCK_DELIVERY_SERVICE_ID)
                        .shipmentDate(supplierShipmentDateTime))
                .itemBuilder(item)
                .property(OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD, true)
        );

        Parameters parameters = CheckoutRequestUtils.shopRequestFor(cart, List.of(offerOf(item)
                        .isFulfillment(false)
                        .atSupplierWarehouse(true)
                        .warehouseId(145)
                        .build()),
                ShopSettingsHelper::getDefaultMeta, optionsConfigurer -> optionsConfigurer.add(item,
                        LocalDeliveryOptionProvider.getMarDoLocalDeliveryOption(
                                MOCK_DELIVERY_SERVICE_ID, 1, supplierShipmentDateTime,
                                Duration.ofHours(23))), null, null, null);

        order = Objects.requireNonNull(orderCreateHelper.createOrder(parameters));

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.PENDING));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    void shouldUpdateStartDateTime() {
        Instant newSupplierShipmentDateTime = supplierShipmentDateTime.plus(1, ChronoUnit.DAYS);
        ZonedDateTime newShipmentDateTimeBySupplier =
                supplierShipmentDateTime.plus(2, ChronoUnit.DAYS).atZone(getClock().getZone());
        ZonedDateTime newReceptionDateTimeByWarehouse =
                supplierShipmentDateTime.plus(3, ChronoUnit.DAYS).atZone(getClock().getZone());
        Instant startDateTime = supplierShipmentDateTime.plus(4, ChronoUnit.DAYS);

        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(
                        MOCK_DELIVERY_SERVICE_ID,
                        2
                ).build();

        List<ActualDeliveryOption> mockedActualDeliveryOptions = actualDelivery.getResults()
                .get(0)
                .getDelivery();
        mockedActualDeliveryOptions.forEach(o -> o.setSupplierProcessings(List.of(
                new SupplierProcessing(
                        145,
                        startDateTime,
                        newSupplierShipmentDateTime,
                        newShipmentDateTimeBySupplier,
                        newReceptionDateTimeByWarehouse
                ))));
        mockedActualDeliveryOptions.forEach(o -> o.setShipmentDateTimeBySupplier(newShipmentDateTimeBySupplier));
        mockedActualDeliveryOptions.forEach(o -> o.setReceptionDateTimeByWarehouse(newReceptionDateTimeByWarehouse));
        ReportGeneratorParameters reportGeneratorParameters = new ReportGeneratorParameters(order, actualDelivery);

        resetWireMocks();
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, reportGeneratorParameters);

        Order order = orderStatusHelper.updateOrderStatus(this.order.getId(), OrderStatus.PROCESSING);

        assertThat(order.getStatus(), is(OrderStatus.PROCESSING));

        Parcel parcel = order.getDelivery().getParcels().get(0);
        assertThat(parcel.getShipmentDateTimeBySupplier(), equalTo(newShipmentDateTimeBySupplier.toLocalDateTime()));
        assertThat(parcel.getReceptionDateTimeByWarehouse(),
                equalTo(newReceptionDateTimeByWarehouse.toLocalDateTime()));

        LocalDateTime shipmentDateTimeBySupplier =
                newShipmentDateTimeBySupplier.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime receptionDateTimeByWarehouse = newReceptionDateTimeByWarehouse.withZoneSameInstant(ZoneId.of(
                "UTC")).toLocalDateTime();
        ParcelItem parcelItem = parcel.getParcelItems().get(0);
        assertThat(parcelItem.getSupplierStartDateTime(), equalTo(startDateTime));
        assertThat(parcelItem.getShipmentDateTimeBySupplier(), equalTo(shipmentDateTimeBySupplier));
        assertThat(parcelItem.getReceptionDateTimeByWarehouse(), equalTo(receptionDateTimeByWarehouse));
    }

}
