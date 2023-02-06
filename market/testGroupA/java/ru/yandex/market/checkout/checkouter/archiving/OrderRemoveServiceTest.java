package ru.yandex.market.checkout.checkouter.archiving;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderRemoveService;
import ru.yandex.market.checkout.checkouter.storage.StorageType;

import static java.util.Collections.emptyMap;

public class OrderRemoveServiceTest extends AbstractArchiveWebTestBase {

    @Autowired
    private OrderRemoveService orderRemoveService;

    @Test
    public void shouldRemoveBlueOrder() {
        Order order = completeOrder();

        orderRemoveService.deleteOrder(order.getId());

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.ARCHIVE));
        assertArchivedTableIdsEquals(emptyMap(), loadArchivedTableIds());
    }

}
