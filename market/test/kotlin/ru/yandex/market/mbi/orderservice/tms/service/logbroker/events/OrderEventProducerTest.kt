package ru.yandex.market.mbi.orderservice.tms.service.logbroker.events

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.producer.AsyncProducer
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logbroker.model.LogbrokerCluster
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.defaultTestMapper
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.orderservice.common.model.yt.KeyValueEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.properties.OutboundLogbrokerProperties
import ru.yandex.market.mbi.orderservice.common.service.pg.PgEnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.YtEnvironmentService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentSkipListSet

@DbUnitDataSet
@CleanupTables(
    [
        OutboundOrderEventEntity::class,
        KeyValueEntity::class
    ]
)
/**
 * Тест для OrderEventLogbrokerProducer
 */
class OrderEventProducerTest : FunctionalTest() {

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var ytEnvironmentService: YtEnvironmentService

    @Autowired
    lateinit var pgEnvironmentService: PgEnvironmentService

    private var logbrokerCluster: LogbrokerCluster? = null

    private var producerJob: OrderEventLogbrokerProducer? = null

    private var asyncProducer: AsyncProducer? = null

    @BeforeEach
    fun init() {
        logbrokerCluster = mock { LogbrokerCluster::class.java }
        producerJob = OrderEventLogbrokerProducer(
            orderRepository = orderRepository,
            lbkxCluster = logbrokerCluster!!,
            orderEventAsyncProducerConfig = mock { AsyncProducer::class.java },
            outboundLogbrokerProperties = OutboundLogbrokerProperties(
                "",
                "",
                999
            ),
            ytEnvironmentService,
            pgEnvironmentService
        )
        asyncProducer = mock { AsyncProducer::class.java }
        val initFutureMock = mock<CompletableFuture<ProducerInitResponse>> { CompletableFuture::class.java }
        whenever(asyncProducer!!.init()).thenReturn(initFutureMock)
        whenever(initFutureMock.get()).thenReturn(null)
        whenever(logbrokerCluster!!.createAsyncProducer(any())).thenReturn(asyncProducer)

        whenever(clock.instant()).thenReturn(Instant.now())
    }

    @Test
    fun `correct write single event`() {
        val events = createEvents("producer/write-single-event-0.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(1)
        assertThat(topicSimulation).containsAll(events.map { it.key.eventId })
        assertThat(duplication).hasSize(0)
    }

    @Test
    fun `correct write multiple events`() {
        val events = createEvents("producer/write-multiple-event-1.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(10)
        assertThat(topicSimulation).containsAll(events.map { it.key.eventId })
        assertThat(duplication).hasSize(0)
    }

    @Test
    fun `verify that event delay is respected`() {
        // в топик должны быть записаны только сообщения до таймстемпа 1600000000000
        whenever(clock.instant()).thenReturn(Instant.ofEpochMilli(1600000000000 + DEFAULT_EVENT_PRODUCER_DELAY_MS))
        whenever(asyncProducer!!.write(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(ProducerWriteResponse(1, 1, false)))
        val events = createEvents("producer/write-multiple-events-2.json")

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L))
            .hasSize(2)
            .containsExactly(
                OutboundOrderEventEntity(
                    key = OutboundOrderEventKey(2),
                    eventTimestamp = Instant.ofEpochMilli(1600000000001),
                    processed = false,
                    orderId = 1,
                    eventPayloadType = "",
                    eventPayload = ByteArray(0)
                ),
                OutboundOrderEventEntity(
                    key = OutboundOrderEventKey(3),
                    eventTimestamp = Instant.ofEpochMilli(1600000040000),
                    processed = false,
                    orderId = 1,
                    eventPayloadType = "",
                    eventPayload = ByteArray(0)
                ),
            )
        verify(asyncProducer!!, times(2)).write(any(), any(), any())
    }

    @Test
    fun `empty unprocessed events`() {
        val events = createEvents("producer/empty-unprocessed-events-2.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(0)
        assertThat(duplication).hasSize(0)
    }

    @Test
    fun `partially unprocessed events`() {
        val events = createEvents("producer/partially-unprocessed-events-3.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(5)
        assertThat(topicSimulation).containsAll(listOf(6, 7, 8, 9, 10))
        assertThat(duplication).hasSize(0)
    }

    @Test
    fun `partially unprocessed events duplication`() {
        val events = createEvents("producer/partially-unprocessed-events-duplication-4.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()
        topicSimulation.add(5)
        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        producerJob!!.runProducer()

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(6)
        assertThat(topicSimulation).containsAll(listOf(5, 6, 7, 8, 9, 10))
        assertThat(duplication).hasSize(2)
    }

    @Test
    fun `partially writting with exception`() {
        val events = createEvents("producer/partially-writting-5.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            if (it >= 5) {
                throw IllegalStateException()
            }

            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        kotlin.runCatching { producerJob!!.runProducer() }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(10)
        assertThat(topicSimulation).hasSize(4)
        assertThat(topicSimulation).containsAll(listOf(1, 2, 3, 4))
        assertThat(duplication).hasSize(0)
    }

    @Test
    fun `partially writting with exception with retry`() {
        val events = createEvents("producer/partially-writting-5.json")
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                events,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            if (it >= 5) {
                throw IllegalStateException()
            }

            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        kotlin.runCatching { producerJob!!.runProducer() }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(10)
        assertThat(topicSimulation).hasSize(4)
        assertThat(topicSimulation).containsAll(listOf(1, 2, 3, 4))
        assertThat(duplication).hasSize(0)

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        kotlin.runCatching { producerJob!!.runProducer() }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(10)
        assertThat(topicSimulation).containsAll(events.map { it.key.eventId })
        assertThat(duplication).hasSize(4)
    }

    @Test
    fun `partially writting`() {
        val events = createEvents("producer/partially-writting-5.json")
        val first = events.filter { it.key.eventId <= 5 }
        val second = events.filter { it.key.eventId > 5 }
        val duplication = HashSet<Long>()

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                first,
                tx
            )
        }

        val topicSimulation = ConcurrentSkipListSet<Long>()

        writeSimulation(asyncProducer!!) {
            val response = insertToTopic(topicSimulation, it)

            if (response.isAlreadyWritten) {
                duplication.add(it)
            }

            response
        }

        kotlin.runCatching { producerJob!!.runProducer() }

        val unprocessed = orderRepository.findUnprocessedOutboundEvents(0, 1000L)
        assertThat(unprocessed).hasSize(0)
        assertThat(topicSimulation).hasSize(5)
        assertThat(topicSimulation).containsAll(first.map { it.key.eventId })
        assertThat(duplication).hasSize(0)

        orderRepository.rwClient.execInTransaction { tx ->
            orderRepository.storeOutboundOrderEvents(
                second,
                tx
            )
        }

        kotlin.runCatching { producerJob!!.runProducer() }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0, 1000L)).hasSize(0)
        assertThat(topicSimulation).hasSize(10)
        assertThat(topicSimulation).containsAll(events.map { it.key.eventId })
        assertThat(duplication).hasSize(0)
    }

    private fun insertToTopic(
        topic: MutableSet<Long>,
        eventId: Long
    ): ProducerWriteResponse {
        synchronized(topic) {
            val maxSeqNo = topic.maxOfOrNull { it }
            if (maxSeqNo != null && maxSeqNo >= eventId) {
                return ProducerWriteResponse(maxSeqNo, -1, true)
            } else {
                topic.add(eventId)
                return ProducerWriteResponse(eventId, -1, false)
            }
        }
    }

    private fun writeSimulation(asyncProducer: AsyncProducer, insertLogic: (Long) -> ProducerWriteResponse) {
        doAnswer { invocation ->
            CompletableFuture.supplyAsync { insertLogic(invocation.arguments[1] as Long) }
        }.whenever(asyncProducer).write(any(), any(), any())
    }

    private fun createEvents(eventsResourceName: String): List<OutboundOrderEventEntity> {
        return defaultTestMapper.readValue<List<OutboundOrderEventEntity>>(
            this::class.loadResourceAsString(
                eventsResourceName
            )
        )
    }
}
