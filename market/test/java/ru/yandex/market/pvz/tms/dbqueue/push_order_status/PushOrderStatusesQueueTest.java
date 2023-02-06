package ru.yandex.market.pvz.tms.dbqueue.push_order_status;

/**
 * @author kukabara
 */

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.dbqueue.push_order_status.PushOrderStatusesProducer;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.ds.client.DeliveryClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author kukabara
 */
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PushOrderStatusesQueueTest {

    private static final String YANDEX_ID = "1";
    private static final String DELIVERY_ID = "11";
    private static final String YANDEX_ID_2 = "2";
    private static final String DELIVERY_ID_2 = "22";
    private static final long DELIVERY_SERVICE_ID = 1L;
    private static final String TOKEN = "XXX";

    private final TestDeliveryServiceFactory deliveryServiceFactory;

    private final PushOrderStatusesProducer pushOrderStatusesProducer;
    private final DbQueueTestUtil dbQueueTestUtil;

    @MockBean
    DeliveryClient deliveryClient;

    @Test
    void testPushOrderStatuses() {
        deliveryServiceFactory.createDeliveryService(TestDeliveryServiceFactory.DeliveryServiceParams.builder()
                .id(DELIVERY_SERVICE_ID)
                .token(TOKEN)
                .build());
        List<DeliveryClient.DsOrderID> orderIds = List.of(
                new DeliveryClient.DsOrderID(YANDEX_ID, DELIVERY_ID),
                new DeliveryClient.DsOrderID(YANDEX_ID_2, DELIVERY_ID_2));

        pushOrderStatusesProducer.produce(orderIds, DELIVERY_SERVICE_ID);
        dbQueueTestUtil.executeSingleQueueItem(PvzQueueType.PUSH_ORDER_STATUSES);
        verify(deliveryClient).pushOrdersStatuses(eq(orderIds), eq(TOKEN));
    }

}
