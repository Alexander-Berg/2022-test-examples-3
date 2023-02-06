package ru.yandex.market.checkout.checkouter.archiving;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.archive.MultiThreadOrdersOperations;

class RemoveArchivedOrdersTaskTest extends AbstractArchiveWebTestBase {

    @Autowired
    private MultiThreadOrdersOperations operations;

    @BeforeEach
    void initProperties() {
        checkouterProperties.setEnableArchivingBulkInsert(false);
    }

    @Test
    @DisplayName("POSITIVE: Успешное удаление заказа из архивной базы")
    void shouldMoveArchivedOrderTest() {
        // создаем заказ и перемещаем его в архивную БД
        Order order = createArchivedOrder();
        Set<Long> ordersIds = new HashSet<>(Collections.singletonList(order.getId()));
        assertSuccessfulTaskRun(ordersIds);
        moveArchivedOrders();

        // удаляем заказ из архивной БД
        operations.removeOrders(ordersIds, OrderMovingDirection.ARCHIVE_TO_NULL, () -> false);

        // проверяем, что заказ удалился
        checkOrderRecordsExistence(StorageType.BASIC, order.getId(), false);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    private void assertSuccessfulTaskRun(@Nonnull Collection<Long> ids) {
        moveArchivedOrders();
        ids.forEach(id -> checkOrderRecordsExistence(StorageType.BASIC, id, false));
    }
}
