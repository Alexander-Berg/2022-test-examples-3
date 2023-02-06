package ru.yandex.market.logistics.les.sqs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractLargeContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.EntityType
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.entity.enums.FlagKey
import ru.yandex.market.logistics.les.entity.ydb.EntityDao
import ru.yandex.market.logistics.les.entity.ydb.EventDaoConverter
import ru.yandex.market.logistics.les.service.FlagService
import ru.yandex.market.logistics.les.util.toInternal

@MockBean(FlagService::class)
class YdbSavingTest : AbstractLargeContextualTest() {

    @Value("\${sqs.queues.test}")
    lateinit var queueName: String

    @Value("\${sqs.queues.ydb}")
    lateinit var ydbQueueName: String

    @Autowired
    lateinit var eventDaoConverter: EventDaoConverter

    @BeforeEach
    fun setUp() {
        whenever(ydbEventRepository.create(any())).thenReturn(1)
        whenever(ydbEntityRepository.create(any(), any())).thenReturn(1)
        createQueues(client, lesSqsProperties)
    }

    @Test
    fun saveEventToYdbSync() {
        setFlagValue(false)
        sendEventAndCheckYdbSaving()

        verify(jmsTemplate, never()).send(eq(ydbQueueName), any())
    }

    @Test
    fun saveEventToYdbViaSqsAsync() {
        setFlagValue(true)
        sendEventAndCheckYdbSaving()

        verify(jmsTemplate).send(eq(ydbQueueName), any())
    }

    private fun sendEventAndCheckYdbSaving() {
        jmsTemplate.convertAndSend(queueName, EVENT)

        val eventDao = eventDaoConverter.fromInternalEvent(INTERNAL_EVENT)
        verify(ydbEventRepository, timeout(10000)).create(eventDao)
        verify(ydbEntityRepository, timeout(10000)).create(EntityDao(null, "123", EntityType.ORDER), eventDao)
    }

    private fun setFlagValue(value: Boolean?) {
        whenever(flagService.getBooleanValue(FlagKey.SAVE_EVENTS_TO_YDB_ASYNC))
            .thenReturn(value)
    }

    companion object {
        private const val SOURCE = "test_source"
        private const val EVENT_TYPE = "test_event_type"

        private val EVENT = Event(
            SOURCE,
            "event_id_3",
            0,
            EVENT_TYPE,
            OrderDamagedEvent("123"),
            null
        )
        private val INTERNAL_EVENT = EVENT.toInternal()
    }
}
