package ru.yandex.market.checkout.checkouter.order.archive;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.storage.archive.repository.OrderArchivingDao;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public class OrderArchiveDaoTest extends AbstractArchiveWebTestBase {

    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    private OrderArchivingDao orderArchivingDao;

    @BeforeEach
    @Override
    public void setUp() throws InterruptedException {
        super.setUp();
        cleanOrders();
    }

    @AfterEach
    public void tearDown() {
        cleanOrders();
    }

    @Test
    public void getOrderIdsWithOffset() {
        int totalCount = 4;
        for (int i = 0; i < totalCount; i++) {
            orderInsertHelper.insertOrder(OrderProvider.getBlueOrder());
        }

        List<Long> orderIds = orderArchivingDao.findOrderIdsWithOffset(totalCount - 1, 0);
        Assertions.assertEquals(totalCount - 1, orderIds.size());

        orderIds = orderArchivingDao.findOrderIdsWithOffset(3, orderIds.get(2));
        Assertions.assertEquals(1, orderIds.size());
    }

    @Test
    public void getMultiOrderIds() {
        orderInsertHelper.insertOrder(OrderProvider.getBlueOrder());
        orderInsertHelper.insertOrder(OrderProvider.getBlueOrder());
        List<Long> multiOrder = insertMultiOrder();
        List<Long> batch = orderArchivingDao.findOrderIdsWithOffset(3, 0);

        List<Long> multiOrderIds = orderArchivingDao.findMultiOrderIds(batch);
        Assertions.assertEquals(2, multiOrderIds.size());
        Assertions.assertEquals(multiOrder.get(0), multiOrderIds.get(0));
        Assertions.assertEquals(multiOrder.get(1), multiOrderIds.get(1));
    }

    @Test
    public void findArchivedMultiOrders() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        exportEvents();

        List<Pair<Long, String>> batch = orderArchivingDao.findArchivedMultiOrders(100, 0, Long.MAX_VALUE,
                Partition.of(0, partitionsCount));

        Assertions.assertEquals(1, batch.size());
        Pair<Long, String> item = batch.get(0);
        Assertions.assertNotNull(item.getFirst());
        Assertions.assertEquals(orders.get(0).getProperty(OrderPropertyType.MULTI_ORDER_ID), item.getSecond());
    }
}
