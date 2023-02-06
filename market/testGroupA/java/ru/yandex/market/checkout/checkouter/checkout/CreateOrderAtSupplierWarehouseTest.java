package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Map;

import com.google.common.collect.Iterables;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;

public class CreateOrderAtSupplierWarehouseTest extends AbstractWebTestBase {

    private final CheckoutContextHolder.CheckoutAttributesHolder checkoutAttributesHolder =
            new CheckoutContextHolder.CheckoutAttributesHolder();

    @BeforeEach
    public void setUp() {
        checkoutAttributesHolder.clear();
    }

    @Test
    public void shouldSaveAtSupplierWarehouseFlag() {
        Parameters parameters = new Parameters();

        OrderItem orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).setAtSupplierWarehouse(true);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItem(orderItem.getFeedOfferId()).getAtSupplierWarehouse(), CoreMatchers.is(true));
    }

    @Test
    public void shouldSaveAtSupplierWarehouseInContext() {
        Parameters parameters = new Parameters();

        OrderItem orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).setAtSupplierWarehouse(true);

        Order order = orderCreateHelper.createOrder(parameters);

        Map<String, Object> attributes = checkoutAttributesHolder.getAttributes();
        assertThat(attributes.get("atSupplierWarehouse"), CoreMatchers.is(true));
    }
}
