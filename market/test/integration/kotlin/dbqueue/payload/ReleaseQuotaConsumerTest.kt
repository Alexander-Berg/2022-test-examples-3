package ru.yandex.market.logistics.calendaring.dbqueue.payload

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.ReleaseQuotaConsumer
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId

class ReleaseQuotaConsumerTest(
    @Autowired private val releaseQuotaConsumer: ReleaseQuotaConsumer
): AbstractContextualTest() {

    @Test
    fun dbqueueConsumerRequestSuccess() {
        val payload = ReleaseQuotaPayload(setOf(1, 2))

        val task: Task<ReleaseQuotaPayload> = Task.builder<ReleaseQuotaPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        releaseQuotaConsumer.execute(task)
    }

}
