package ru.yandex.market.checkout.checkouter.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class OrderPartitionMappingTest extends AbstractWebTestBase {

    @Test
    @DisplayName("POSITIVE: В событиях заполнено поле partitionIndex для заказа")
    void partitionIndexEventTest() {
        Order orderWithPartition = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        Order orderWithoutPartition = orderCreateHelper.createOrder(defaultBlueOrderParameters());

        clearPartition(orderWithoutPartition);

        getEvents(orderWithPartition).getItems().forEach(
                event -> assertNotNull(event.getOrderAfter().getPartitionIndex())
        );
        getEvents(orderWithoutPartition).getItems().forEach(
                event -> assertNull(event.getOrderAfter().getPartitionIndex())
        );
    }

    private PagedEvents getEvents(Order orderWithPartition) {
        return eventService.getPagedOrderHistoryEvents(
                orderWithPartition.getId(),
                Pager.atPage(1, 1000),
                null,
                null,
                null,
                false,
                ClientInfo.SYSTEM,
                null
        );
    }

    private void clearPartition(Order order) {
        transactionTemplate.execute(
                ts -> masterJdbcTemplate.update("update orders set partition_index = null where id = ?", order.getId())
        );
    }

}
