package ru.yandex.market.logistics.les.service.sqs

import com.amazonaws.services.sqs.model.CreateQueueResult
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.entity.Source
import ru.yandex.market.logistics.les.service.SubscriptionService

class SubscriptionTest : AbstractContextualTest() {

    @Autowired
    lateinit var subscriptionService: SubscriptionService

    @Test
    @DatabaseSetup("/sqs/subscribe/before/setup.xml")
    @ExpectedDatabase(
        value = "/sqs/subscribe/after/first_subscribe.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun subscriptionTest() {
        whenever(sqsClient.createQueue(QUEUE_NAME)).thenReturn(CreateQueueResult().withQueueUrl(QUEUE_URL))
        whenever(sqsClient.createQueue("${QUEUE_NAME}_dlq"))
            .thenReturn(CreateQueueResult().withQueueUrl("${QUEUE_URL}_dlq"))

        val (queueName, resQueueUrl) = subscriptionService.subscribe(SUBSCRIBER, SOURCE, EVENT_TYPE)
        assertEquals(QUEUE_NAME, queueName)
        assertEquals(QUEUE_URL, resQueueUrl)
    }

    @Test
    @DatabaseSetup(
        "/sqs/subscribe/before/setup.xml",
        "/sqs/subscribe/before/already_exist.xml",
    )
    @ExpectedDatabase(
        value = "/sqs/subscribe/before/already_exist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun alreadyExist() {
        val (queueName, resQueueUrl) = subscriptionService.subscribe(SUBSCRIBER, SOURCE, EVENT_TYPE)
        assertEquals(QUEUE_NAME, queueName)
        assertEquals(QUEUE_URL, resQueueUrl)
    }

    @Test
    @DatabaseSetup(
        "/sqs/subscribe/before/setup_two_sources.xml",
        "/sqs/subscribe/before/subscription_with_another_source.xml",
    )
    @ExpectedDatabase(
        value = "/sqs/subscribe/after/two_subscription_different_sources.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun newSource() {
        val result = CreateQueueResult()
        result.queueUrl = QUEUE_URL
        whenever(sqsClient.createQueue(QUEUE_NAME)).thenReturn(result)

        val (queueName, resQueueUrl) = subscriptionService.subscribe(SUBSCRIBER, SOURCE, EVENT_TYPE)
        assertEquals(QUEUE_NAME, queueName)
        assertEquals(QUEUE_URL, resQueueUrl)
    }

    @Test
    @DatabaseSetup(
        "/sqs/subscribe/before/setup_two_sources.xml",
        "/sqs/subscribe/before/subscription_with_another_type.xml",
    )
    @ExpectedDatabase(
        value = "/sqs/subscribe/after/two_subscription_different_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun newEventType() {
        val result = CreateQueueResult()
        result.queueUrl = QUEUE_URL
        whenever(sqsClient.createQueue(QUEUE_NAME)).thenReturn(result)

        val (queueName, resQueueUrl) = subscriptionService.subscribe(SUBSCRIBER, SOURCE, EVENT_TYPE)
        assertEquals(QUEUE_NAME, queueName)
        assertEquals(QUEUE_URL, resQueueUrl)
    }

    companion object {
        private const val SUBSCRIBER = "test_subscriber"
        private const val QUEUE_NAME = SUBSCRIBER + "_in"
        private val SOURCE = Source(
            name = "test_source",
            id = 1,
        )
        private const val EVENT_TYPE = "test_event_type"
        private const val QUEUE_URL = "http://localhost:1234/queue/$QUEUE_NAME"
    }
}
