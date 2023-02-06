package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor.QueuedCallExecution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CalculateOutletStorageLimitDateProcessorTest extends AbstractServicesTestBase {

    @Autowired
    CalculateOutletStorageLimitDateProcessor calculateOutletStorageLimitDateProcessor;

    @Autowired
    OrderServiceHelper orderServiceHelper;

    @Test
    public void shouldCaluclateAndWriteOutletStorageLimitDate() {
        //given:
        var order = OrderProvider.getBlueOrder();
        var delivery = DeliveryProvider.getShopDeliveryWithPickupType();
        int storagePeriod = 3;
        delivery.setOutletStoragePeriod(storagePeriod);
        order.setDelivery(delivery);
        orderServiceHelper.saveOrder(order);
        var callCreationDate = LocalDate.of(2020, 1, 17);
        var queuedCallExecution = new QueuedCallExecution(order.getId(), null, 1,
                callCreationDate.atTime(12, 0).toInstant(ZoneOffset.UTC), order.getId());
        var expectedDate = callCreationDate.plusDays(storagePeriod);

        //when:
        ExecutionResult result = calculateOutletStorageLimitDateProcessor.process(queuedCallExecution);

        //then:
        assertThat(result, equalTo(ExecutionResult.SUCCESS));
        Order loadedOrder = orderService.getOrder(order.getId());
        assertThat(loadedOrder.getDelivery().getOutletStorageLimitDate(), equalTo(expectedDate));
    }

    @Test
    public void shouldNotFailIfDeliveryDateIsInPast() {
        //given:
        var order = OrderProvider.getBlueOrder();
        var delivery = DeliveryProvider.getShopDeliveryWithPickupType();
        DeliveryDates deliveryDates = DeliveryProvider.getDeliveryDates(
                LocalDateTime.of(2020, 1, 17, 12, 0), LocalDateTime.of(2020, 1, 17, 18, 0));
        delivery.setDeliveryDates(deliveryDates);
        int storagePeriod = 3;
        delivery.setOutletStoragePeriod(storagePeriod);
        order.setDelivery(delivery);
        orderServiceHelper.saveOrder(order);
        var callCreationDate = LocalDate.of(2020, 1, 17);
        var queuedCallExecution = new QueuedCallExecution(order.getId(), null, 1,
                callCreationDate.atTime(12, 0).toInstant(ZoneOffset.UTC), order.getId());
        var expectedDate = callCreationDate.plusDays(storagePeriod);

        //when:
        ExecutionResult result = calculateOutletStorageLimitDateProcessor.process(queuedCallExecution);

        //then:
        assertThat(result, equalTo(ExecutionResult.SUCCESS));
        Order loadedOrder = orderService.getOrder(order.getId());
        assertThat(loadedOrder.getDelivery().getOutletStorageLimitDate(), equalTo(expectedDate));
    }

    @Test
    @DisplayName("Попытка обработать заказ в статусе CANCELLED")
    public void shouldNotFailIfOrderStatusCancelled() {
        //given:
        var order = OrderProvider.getBlueOrder();
        var delivery = DeliveryProvider.getShopDeliveryWithPickupType();
        int storagePeriod = 3;
        delivery.setOutletStoragePeriod(storagePeriod);
        order.setDelivery(delivery);
        order.setStatus(OrderStatus.CANCELLED);
        orderServiceHelper.saveOrder(order);
        var callCreationDate = LocalDate.of(2020, 1, 17);
        var queuedCallExecution = new QueuedCallExecution(order.getId(), null, 1,
                callCreationDate.atTime(12, 0).toInstant(ZoneOffset.UTC), order.getId());
        var expectedDate = callCreationDate.plusDays(storagePeriod);

        //when:
        ExecutionResult result = calculateOutletStorageLimitDateProcessor.process(queuedCallExecution);

        //then:
        assertThat(result, equalTo(ExecutionResult.SUCCESS));
        Order loadedOrder = orderService.getOrder(order.getId());
        assertThat(loadedOrder.getDelivery().getOutletStorageLimitDate(), equalTo(expectedDate));
    }
}

