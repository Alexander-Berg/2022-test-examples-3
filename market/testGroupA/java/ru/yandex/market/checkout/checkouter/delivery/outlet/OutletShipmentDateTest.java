package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OutletShipmentDateTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void shouldReturnOutletStoragePeriodAndLimitDateForDefaultBlueOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);

        Order order = orderCreateHelper.createOrder(parameters);

        proceedToPickupAndCheckDeliveryOutletStorageLimitDate(order);
    }

    @Test
    public void shouldReturnOutletStoragePeriodAndLimitDateForDropShip() throws Exception {
        Parameters dropshipPrepaidParameters = DropshipDeliveryHelper.getDropshipPrepaidParameters(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(dropshipPrepaidParameters);
        assertThat(OrderTypeUtils.isFulfilment(order), Is.is(false));
        assertThat(OrderTypeUtils.isMarketDelivery(order), Is.is(true));

        proceedToPickupAndCheckDeliveryOutletStorageLimitDate(order);
    }

    private void proceedToPickupAndCheckDeliveryOutletStorageLimitDate(Order order) {
        //хардкод в outlet.xml
        final int storagePeriod = 1;

        order = orderService.getOrder(order.getId());

        assertThat(order.getDelivery().getOutletStoragePeriod(), is(storagePeriod));
        assertNull(order.getDelivery().getOutletStorageLimitDate());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);


        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CALCULATE_STORAGE_LIMIT_DATE);
        order = orderService.getOrder(order.getId());

        assertEquals(order.getDelivery().getOutletStorageLimitDate(),
                LocalDate.now().plus(storagePeriod, ChronoUnit.DAYS));
    }
}
