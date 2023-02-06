package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MigrateFulfillmentWarehouseIdCommandTest extends AbstractWebTestBase {

    @Autowired
    private MigrateFulfillmentWarehouseIdCommand command;
    private Order order;

    @BeforeEach
    public void init() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        isFulfulmentWarehouseIdNotNull();

        transactionTemplate.execute(st -> masterJdbcTemplate.update(
                "update order_item set fulfilment_warehouse_id=null where order_id = " + order.getId()));
        isFulfilmentWarehouseIdNull();
    }

    @Test
    public void migrateWithoutArgs() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        command.executeCommand(
                new CommandInvocation(
                        "migrate-fulfillment-warehouse-id",
                        new String[]{},
                        Collections.emptyMap()),
                new TestTerminal(new ByteArrayInputStream(new byte[0]), output)
        );
        isFulfulmentWarehouseIdNotNull();
    }

    @Test
    public void migrateWithBatchSizeArg() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        command.executeCommand(
                new CommandInvocation(
                        "migrate-fulfillment-warehouse-id",
                        new String[]{"1"},
                        Collections.emptyMap()),
                new TestTerminal(new ByteArrayInputStream(new byte[0]), output)
        );
        isFulfulmentWarehouseIdNotNull();
    }

    @Test
    public void migrateOnlyRecentOrders() {
        executeCommandForOrdersCreatedAfter("2019-09-15T00:00:00");
        isFulfulmentWarehouseIdNotNull();
    }

    @Test
    public void notMigrateDeliveredOrder() {
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        executeCommandForOrdersCreatedAfter("2019-09-15T00:00:00");
        isFulfilmentWarehouseIdNull();
    }

    @Test
    public void notMigrateCancelledOrder() {
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        executeCommandForOrdersCreatedAfter("2019-09-15T00:00:00");
        isFulfilmentWarehouseIdNull();
    }

    @Test
    public void notMigrateOldOrders() {
        executeCommandForOrdersCreatedAfter(LocalDateTime.now().toString());
        isFulfilmentWarehouseIdNull();
    }

    private void isFulfulmentWarehouseIdNotNull() {
        Map<String, Object> result = masterJdbcTemplate.queryForMap(
                "select fulfilment_warehouse_id, warehouse_id from order_item where order_id  = " + order.getId());
        assertNotNull(result.get("fulfilment_warehouse_id"));
        assertNotNull(result.get("warehouse_id"));
        Long fulfilmentWarehouseId = Long.valueOf(result.get("fulfilment_warehouse_id").toString());
        Long warehouseId = Long.valueOf(result.get("warehouse_id").toString());
        assertThat(warehouseId, equalTo(300501L));
        assertThat(fulfilmentWarehouseId, equalTo(warehouseId));
    }

    private void isFulfilmentWarehouseIdNull() {
        Map<String, Object> result = masterJdbcTemplate.queryForMap(
                "select fulfilment_warehouse_id from order_item where order_id  = " + order.getId());
        assertNull(result.get("fulfilment_warehouse_id"));
    }

    private void executeCommandForOrdersCreatedAfter(String createdAt) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        command.executeCommand(
                new CommandInvocation(
                        "migrate-fulfillment-warehouse-id",
                        new String[]{"1", createdAt},
                        Collections.emptyMap()),
                new TestTerminal(new ByteArrayInputStream(new byte[0]), output)
        );
    }
}
