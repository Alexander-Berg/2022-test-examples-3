package ru.yandex.market.logistics.les.queue

import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.client.configuration.properties.RedrivePolicy
import ru.yandex.market.logistics.les.service.queue.consumer.ConfigureDlqTaskConsumer
import ru.yandex.market.logistics.les.service.queue.dto.ConfigureDlqDto
import ru.yandex.market.logistics.les.service.queue.producer.ConfigureDlqTaskProducer
import ru.yandex.money.common.dbqueue.api.EnqueueParams

class DbQueueDlqConfiguringTest : AbstractContextualTest() {

    @Autowired
    lateinit var configureDlqTaskProducer: ConfigureDlqTaskProducer

    @Autowired
    lateinit var configureDlqTaskConsumer: ConfigureDlqTaskConsumer

    @Test
    @ExpectedDatabase(
        value = "/queue/dlq/after/created_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun produceTaskTest() {
        configureDlqTaskProducer.enqueue(EnqueueParams.create(ConfigureDlqDto(QUEUE, QUEUE_DLQ)))
    }

    @Test
    fun sentRequestOnDlqConfiguringTest() {
        whenever(sqsClient.getQueueAttributes(any())).thenReturn(
            GetQueueAttributesResult().withAttributes(
                mapOf<String, String>("QueueArn" to DLQ_ARN)
            )
        )

        configureDlqTaskConsumer.processPayload(ConfigureDlqDto(QUEUE, QUEUE_DLQ))

        verify(sqsClient).getQueueAttributes(GetQueueAttributesRequest(QUEUE_DLQ, listOf("QueueArn")))
        verify(sqsClient).setQueueAttributes(
            SetQueueAttributesRequest()
                .withQueueUrl(QUEUE)
                .addAttributesEntry(
                    "RedrivePolicy",
                    RedrivePolicy(DLQ_ARN, 3).toString()
                )
        )
    }

    companion object {
        private const val QUEUE = "q"
        private const val QUEUE_DLQ = "q_dlq"
        private const val DLQ_ARN = "Never gonna give you up"
    }
}
