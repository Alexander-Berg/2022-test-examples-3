package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class OrderToExtendedOrdersTransformerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private OrderToExtendedOrdersTransformer orderToExtendedOrdersTransformer;

    @Before
    public void setup() {
        orderToExtendedOrdersTransformer = new OrderToExtendedOrdersTransformer();
    }

    @Test
    public void testSingleParcelOrderToDsmOrderTransformer() {
        assertThat(orderToExtendedOrdersTransformer.transform(getExtendedOrderSingleParcel()).size())
            .as("Check size of transformed ExtendedOrder (must be single)").isEqualTo(1);
    }

    @Test
    public void testMultipleParcelOrderToDsmOrderTransformer() {
        assertThat(orderToExtendedOrdersTransformer.transform(getExtendedOrderMultiParcel()).size())
            .as("Check size of transformed ExtendedOrder (must be multiple)").isGreaterThan(1);
    }

    private ExtendedOrder getExtendedOrderSingleParcel() {
        ExtendedOrder extendedOrder = new ExtendedOrder();

        Order order = new Order();
        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(new Parcel()));
        order.setDelivery(delivery);

        extendedOrder.setOrder(order);

        return extendedOrder;
    }

    private ExtendedOrder getExtendedOrderMultiParcel() {
        ExtendedOrder extendedOrder = new ExtendedOrder();

        Order order = new Order();
        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(new Parcel(), new Parcel()));
        order.setDelivery(delivery);

        extendedOrder.setOrder(order);

        return extendedOrder;
    }
}
