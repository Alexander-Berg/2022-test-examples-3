package ru.yandex.market.sc.tms.dbqueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.dbqueue.juggler.return_registry.SendReturnRegistryFailedProducer;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.juggler.JugglerNotificationClient;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@EmbeddedDbTmsTest
public class SendReturnRegistryFailedQueueTest {
    @Autowired
    SendReturnRegistryFailedProducer sendReturnRegistryFailedProducer;

    @Autowired
    DbQueueTestUtil dbQueueTestUtil;

    @Autowired
    TestFactory testFactory;

    @MockBean
    JugglerNotificationClient jugglerNotificationClient;

    @Test
    void sendMessageCourierWhenBatchRegisterReady() {
        SortingCenter sortingCenter = testFactory.storedSortingCenter();
        Warehouse warehouse = testFactory.storedWarehouse();
        sendReturnRegistryFailedProducer.produce(sortingCenter, warehouse);
        dbQueueTestUtil.assertQueueHasSingleEvent(ScQueueType.SEND_RETURN_REGISTRY_FAILED,
                sortingCenter.getId() + "-" + warehouse.getIncorporation());
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.SEND_RETURN_REGISTRY_FAILED);

        verify(jugglerNotificationClient).pushReturnRegistryFailedEvent(
                eq(sortingCenter), eq(warehouse.getIncorporation()));
    }
}
