package ru.yandex.market.logistics.les.service.sqs

import com.amazonaws.services.sqs.model.CreateQueueResult
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.lastValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.service.SourceService

@DisplayName("Создание источника")
class SourceServiceTest : AbstractContextualTest() {

    @Autowired
    lateinit var sourceService: SourceService

    @Test
    @ExpectedDatabase(
        value = "/services/source-service/after/create-source.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSourceTest() {
        whenever(sqsClient.createQueue(QUEUE_NAME)).thenReturn(CreateQueueResult().withQueueUrl(QUEUE_URL))
        whenever(sqsClient.createQueue("${QUEUE_NAME}_dlq"))
            .thenReturn(CreateQueueResult().withQueueUrl("${QUEUE_URL}_dlq"))

        val argumentCaptor = ArgumentCaptor.forClass(QUEUE_NAME.javaClass)
        sourceService.createSource(SOURCE, "description")

        verify(sqsClient, times(2)).createQueue(argumentCaptor.capture())
        assertEquals(QUEUE_NAME, argumentCaptor.firstValue)
        assertEquals(QUEUE_NAME + "_dlq", argumentCaptor.lastValue)
    }

    companion object {
        private const val SOURCE = "test_source"
        private const val QUEUE_NAME = SOURCE + "_out"
        private const val QUEUE_URL = "http://localhost:1234/queue/$QUEUE_NAME"
    }
}
