package ru.yandex.market.wms.shared.libs.async.retry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DeadQueueMessageTtlTest {

    @Test
    void ofInstantiateValidDeadQueueMessageTtl() {
        Assertions.assertEquals(DeadQueueMessageTtl.NONE, DeadQueueMessageTtl.of(0L));
        Assertions.assertEquals(DeadQueueMessageTtl.TWO_HOURS, DeadQueueMessageTtl.of(2 * 60 * 60L));
        Assertions.assertThrows(EnumConstantNotPresentException.class, () -> DeadQueueMessageTtl.of(1L));
    }
}
