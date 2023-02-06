package ru.yandex.market.checkout.checkouter.delivery;

import java.util.Date;

import io.qameta.allure.Epic;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 30.08.17.
 */
public class DeliveryActualizerIntegrationTest extends AbstractWebTestBase {

    private Parameters parameters;
    private Delivery delivery;

    @BeforeEach
    public void setUp() throws Exception {
        parameters = new Parameters();
        parameters.setCheckCartErrors(false);
        delivery = parameters.getBuiltMultiCart().getCarts().get(0).getDelivery();
    }


    @Epic(Epics.CHECKOUT)
    @DisplayName("Чекаутер должен возвращать Changes.DELIVERY, если fromDate в прошлом")
    @Test
    public void actualizeDeliveryDates_old_dates() throws Exception {
        Date fromDate = DateUtils.addDays(new Date(), -1);
        actualizeAndExpectChange(fromDate, fromDate);
    }


    @Epic(Epics.CHECKOUT)
    @DisplayName("Чекаутер должен возвращать Changes.DELIVERY, если toDate < fromDate")
    @Test
    public void actualizeDeliveryDates_to_date_older() throws Exception {
        Date fromDate = DateUtils.addDays(new Date(), 1);
        Date toDate = DateUtils.addHours(fromDate, -1);
        actualizeAndExpectChange(fromDate, toDate);
    }

    private void actualizeAndExpectChange(Date fromDate, Date toDate) throws Exception {
        delivery.setDeliveryDates(new DeliveryDates(fromDate, toDate));
        Order order = orderCreateHelper.cart(parameters).getCarts().get(0);
        assertEquals(1, order.getChanges().size());
        CartChange cartChange = order.getChanges().iterator().next();
        assertEquals(CartChange.DELIVERY, cartChange);
    }

    @Test
    public void actualizeDeliveryDates_actual() throws Exception {
        Order order = orderCreateHelper.cart(parameters).getCarts().get(0);
        assertTrue(CollectionUtils.isEmpty(order.getChanges()));
    }
}
