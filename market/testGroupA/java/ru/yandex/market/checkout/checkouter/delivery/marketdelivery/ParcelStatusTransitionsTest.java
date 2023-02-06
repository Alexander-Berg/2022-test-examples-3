package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.CREATED;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.ERROR;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.NEW;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.UNKNOWN;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;


public class ParcelStatusTransitionsTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return Arrays.asList(
                buildCase(true, CREATED, ERROR),
                buildCase(true, CREATED, READY_TO_SHIP, ERROR),
                buildCase(true, ERROR, NEW),
                buildCase(true, ERROR, CREATED),
                buildCase(true, ERROR, READY_TO_SHIP),
                buildCase(false, READY_TO_SHIP),
                buildCase(false, CREATED, NEW),
                buildCase(false, CREATED, READY_TO_SHIP, NEW),
                buildCase(false, CREATED, READY_TO_SHIP, CREATED),
                buildCase(false, UNKNOWN),
                buildCase(false, CREATED, UNKNOWN),
                buildCase(false, CREATED, READY_TO_SHIP, UNKNOWN)
        ).stream().map(Arguments::of);
    }

    private static Object[] buildCase(boolean shouldBeValid, ParcelStatus... statuses) {
        return new Object[]{
                "NEW -> " + Arrays.stream(statuses)
                        .map(s -> s != null ? s.name() : "null")
                        .collect(Collectors.joining(" -> ")),
                Arrays.asList(statuses),
                shouldBeValid
        };
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkStatusTransitions(String caseName, List<ParcelStatus> statuses, boolean shouldBeValid) {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        for (int index = 0; index < statuses.size() - 1; ++index) {
            order = changeStatus(order, statuses.get(index), true);
        }
        changeStatus(order, statuses.get(statuses.size() - 1), shouldBeValid);
    }

    private Order changeStatus(Order order, ParcelStatus status, boolean shouldBeValid) {
        Delivery delivery = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setStatus(status);
        delivery.setParcels(Collections.singletonList(parcel));
        try {
            order = client.updateOrderDelivery(order.getId(), ClientRole.SYSTEM, 0L, delivery);
            assertEquals(status, Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus());
            if (!shouldBeValid) {
                fail("Status should not be changed!");
            }
        } catch (Exception e) {
            if (shouldBeValid) {
                fail("Status should be changed!");
            }
            order = null;
        }
        return order;
    }
}
