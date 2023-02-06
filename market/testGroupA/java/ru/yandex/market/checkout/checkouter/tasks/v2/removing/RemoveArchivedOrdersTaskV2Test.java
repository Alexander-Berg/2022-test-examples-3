package ru.yandex.market.checkout.checkouter.tasks.v2.removing;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TaskPropertiesDao;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TaskPropertiesService;

public class RemoveArchivedOrdersTaskV2Test extends AbstractArchiveWebTestBase {
    @Autowired
    private RemoveArchivedOrdersTaskV2 removeArchivedOrdersTaskV2;
    @Autowired
    private TaskPropertiesDao taskPropertiesDao;
    @Autowired
    private TaskPropertiesService taskPropertiesService;

    @Test
    public void testTask() {
        Order archivedOrder1 = createArchivedOrder();
        Order archivedOrder2 = createArchivedOrder();
        Set<Long> archivedOrders = Set.of(archivedOrder1.getId(), archivedOrder2.getId());

        taskPropertiesDao.save(removeArchivedOrdersTaskV2.getTaskName());
        taskPropertiesService.setPayload(removeArchivedOrdersTaskV2.getTaskName(), archivedOrders);

        moveArchivedOrders();
        checkOrderRecordsExistence(StorageType.ARCHIVE, archivedOrder1.getId(), true);
        checkOrderRecordsExistence(StorageType.ARCHIVE, archivedOrder2.getId(), true);

        removeArchivedOrdersTaskV2.run(TaskRunType.ONCE);

        checkOrderRecordsExistence(StorageType.ARCHIVE, archivedOrder1.getId(), false);
        checkOrderRecordsExistence(StorageType.ARCHIVE, archivedOrder2.getId(), false);
    }

}
