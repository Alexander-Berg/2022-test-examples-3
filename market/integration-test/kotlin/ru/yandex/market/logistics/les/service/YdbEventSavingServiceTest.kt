package ru.yandex.market.logistics.les.service

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EntityType
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.entity.ydb.EntityDao
import ru.yandex.market.logistics.les.entity.ydb.EventDaoConverter
import ru.yandex.market.logistics.les.repository.ydb.YdbEntityRepository
import ru.yandex.market.logistics.les.repository.ydb.YdbEventRepository
import ru.yandex.market.logistics.les.util.toInternal

class YdbEventSavingServiceTest : AbstractContextualTest() {

    @MockBean
    lateinit var entityStorage: YdbEntityRepository

    @MockBean
    lateinit var eventStorage: YdbEventRepository

    @Autowired
    lateinit var ydbEventSavingService: YdbEventSavingService

    @Autowired
    lateinit var eventDaoConverter: EventDaoConverter

    @Value("\${sqs.queues.ydb}")
    lateinit var ydbQueue: String

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(eventStorage, entityStorage)
    }

    @Test
    fun saveEventToYdb() {
        ydbEventSavingService.saveEventToYdbSync(INTERNAL_EVENT)

        val eventDao = eventDaoConverter.fromInternalEvent(INTERNAL_EVENT)
        verify(eventStorage).create(eventDao)
        verify(entityStorage).create(
            EntityDao(null, TEST_ENTITY.entityId, TEST_ENTITY.entityType),
            eventDao
        )
    }

    @Test
    fun saveEventToYdbFallback() {
        ydbEventSavingService.saveEventToYdbSync(INTERNAL_EVENT)

        val eventDao = eventDaoConverter.fromInternalEvent(INTERNAL_EVENT)
        verify(eventStorage).create(eventDao)
        verify(entityStorage).create(
            EntityDao(null, TEST_ENTITY.entityId, TEST_ENTITY.entityType),
            eventDao
        )
    }

    @Test
    fun saveEventToYdbLegacy() {
        val testInternalEvent = Event(
            source = "test_source_unknown",
            eventId = "event_id_3",
            timestamp = 0,
            eventType = EVENT_TYPE,
            payload = OrderDamagedEvent("123"),
            description = null,
        ).toInternal()
        ydbEventSavingService.saveEventToYdbSync(testInternalEvent)

        val eventDao = eventDaoConverter.fromInternalEvent(testInternalEvent)
        verify(eventStorage).create(eventDao)
        verify(entityStorage).create(EntityDao(null, "123", EntityType.ORDER), eventDao)
    }

    @Test
    fun saveEventToYdbAsync() {
        ydbEventSavingService.saveEventToYdbAsync(INTERNAL_EVENT)

        verify(jmsTemplate).send(eq(ydbQueue), any())
    }

    @Test
    @ExpectedDatabase(
        value = "/services/ydb-saving/after/dbqueue_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveEventToYdbAsyncViaDbqueue() {
        whenever(jmsTemplate.send(anyString(), any()))
            .thenThrow(RuntimeException("Some exception"))

        ydbEventSavingService.saveEventToYdbAsync(INTERNAL_EVENT)

        verify(jmsTemplate).send(eq(ydbQueue), any())
    }

    companion object {
        private const val SOURCE = "test_source"
        private const val EVENT_TYPE = "test_event_type"
        private val TEST_ENTITY = EntityKey(
            entityId = "from_client",
            entityType = EntityType.ORDER,
        )

        private val INTERNAL_EVENT = Event(
            source = SOURCE,
            eventId = "event_id_3",
            timestamp = 0,
            eventType = EVENT_TYPE,
            payload = OrderDamagedEvent("123"),
            description = null,
            entityKeys = listOf(TEST_ENTITY)
        ).toInternal()
    }
}
