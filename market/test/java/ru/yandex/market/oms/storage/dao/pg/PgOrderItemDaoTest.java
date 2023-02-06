package ru.yandex.market.oms.storage.dao.pg;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.oms.AbstractFunctionalTest;
import ru.yandex.market.oms.service.OrderItemService;
import ru.yandex.market.oms.storage.dao.OrderItemDao;
import ru.yandex.market.oms.util.DbTestUtils;
import ru.yandex.mj.generated.server.model.OrderItem;
import ru.yandex.mj.generated.server.model.OrderItemInstance;

@ActiveProfiles("functionalTest")
public class PgOrderItemDaoTest extends AbstractFunctionalTest {

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    OrderItemDao orderItemDao;
    @Autowired
    OrderItemService orderItemService;

    @Autowired
    DbTestUtils dbTestUtils;

    private final Long orderId = 1L;
    private final Long userId = 1L;
    private final Long itemId = 1L;
    private final Long deliveryId = 1L;

    @BeforeEach
    public void beforeEach() {
        dbTestUtils.insertOrder(orderId, userId, deliveryId, OrderStatus.PROCESSING);
        dbTestUtils.insertOrderItem(orderId, itemId);
    }

    @AfterEach
    public void afterEach() {
        dbTestUtils.deleteAllOrderItems(orderId);
        dbTestUtils.deleteOrder(orderId);
    }

    @Test
    @DisplayName("Обновление КИЗов для позиции заказа")
    public void updateInstancesTest() {
        // input
        var sn = "123";
        var snInstance = new OrderItemInstance();
        snInstance.setSn(sn);

        var imei = "some-imei";
        var imeiInstance = new OrderItemInstance();
        imeiInstance.setImei(imei);
        var orderId = this.orderId;

        var orderItems = List.of(new OrderItem().id(itemId).instances(List.of(snInstance, imeiInstance)));

        // prechecks
        checkUpdated(orderId, false, sn, imei);

        // testing actions
        orderItemDao.updateOrderItemsInstances(orderId, orderItems);

        // postchecks
        checkUpdated(orderId, true, sn, imei);
    }

    private void checkUpdated(long orderId, boolean updated, String sn, String imei) {
        var item = orderItemService.getItems(orderId).stream().findFirst().get();
        var updatedSuccessfully = item.getInstances().stream()
                .allMatch(instance -> sn.equals(instance.getSn()) || imei.equals(instance.getImei()));
        Assertions.assertEquals(updated, updatedSuccessfully);
    }
}
