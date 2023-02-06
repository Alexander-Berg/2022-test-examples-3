package ru.yandex.market.checkout.checkouter.order.archive;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.storage.archive.OrderChangeIds;

public class StorageOrderArchiveServiceTest extends AbstractArchiveWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(StorageOrderArchiveServiceTest.class);
    @Autowired
    private OrderArchiveService orderArchiveService;

    @Test
    @Disabled
    /*
     * Пример почему этот тест работает неверно:
     * [2022-06-21 22:02:24,059] INFO  [][main][StorageOrderArchiveServiceTest] Order id: 160, history id: 1018~
     * [2022-06-21 22:02:24,059] INFO  [][main][StorageOrderArchiveServiceTest] Order id: 161, history id: 1019~
     * [2022-06-21 22:02:24,059] INFO  [][main][StorageOrderArchiveServiceTest] Order id: 162, history id: 1024~
     *
     * [2022-06-21 22:02:24,060] INFO  [][main][StorageOrderArchiveServiceTest] Max history id: 1024, last history id: 1024~
     *
     * Полная паста: https://paste.yandex-team.ru/10049456
     */
    public void findOrdersLastChangesBetween() {
        insertMultiOrder();
        insertMultiOrder();

        setFixedTime(getClock().instant().plus(100, ChronoUnit.DAYS));

        long maxHistoryId = orderArchiveService.getMaxHistoryIdForOrdersArchiving().orElseThrow();

        List<OrderChangeIds> changes = orderArchiveService.findOrdersLastChangesBetween(0, maxHistoryId, 3);
        Assertions.assertEquals(3, changes.size());
        changes.forEach(c ->
                LOG.info("Order id: {}, history id: {}", c.getOrderId(), c.getHistoryId())
        );
        long lastHistoryId = changes.stream().mapToLong(OrderChangeIds::getHistoryId).max().orElseThrow();
        List<OrderChangeIds> restChanges = orderArchiveService.findOrdersLastChangesBetween(lastHistoryId,
                maxHistoryId, 3);

        LOG.info("Max history id: {}, last history id: {}", maxHistoryId, lastHistoryId);

        Assertions.assertEquals(1, restChanges.size());
    }
}
