package ru.yandex.market.sc.tms.dbqueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.dbqueue.batch_register.BatchRegisterReadyProducer;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@EmbeddedDbTmsTest
public class BatchRegisterReadyQueueTest {

    @Autowired
    BatchRegisterReadyProducer batchRegisterReadyProducer;

    @Autowired
    DbQueueTestUtil dbQueueTestUtil;

    @MockBean
    TplClient tplClient;

    @Test
    void sendMessageCourierWhenBatchRegisterReady() {
        String batchRegister = "<batch-register>";
        batchRegisterReadyProducer.produce(batchRegister);
        dbQueueTestUtil.assertQueueHasSingleEvent(ScQueueType.BATCH_REGISTER_READY, batchRegister);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.BATCH_REGISTER_READY);

        verify(tplClient).sendCourierBatchReady(eq(batchRegister));
    }
}
