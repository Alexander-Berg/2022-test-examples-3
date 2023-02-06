package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.consumer.CombinatorRouteWasUpdatedConsumer.Companion.SUPPORTED_PRODUCERS
import ru.yandex.market.logistics.mqm.queue.consumer.CombinatorRouteWasUpdatedConsumer.Companion.SUPPORTED_STATUSES
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderCombinatorRouteUpdatedDto
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.OrderLockService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.LogisticEventService
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import ru.yandex.market.logistics.mqm.utils.joinInOrder

@ExtendWith(MockitoExtension::class)
class CombinatorRouteWasUpdatedConsumerTest {

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    lateinit var transactionTemplate: TransactionOperations

    @Mock
    lateinit var planFactService: PlanFactService

    @Mock
    lateinit var lomOrderService: LomOrderService

    @Mock
    lateinit var eventService: LogisticEventService

    @Mock
    lateinit var orderLockService: OrderLockService

    lateinit var consumer: CombinatorRouteWasUpdatedConsumer

    @BeforeEach
    private fun setUp() {
        consumer = CombinatorRouteWasUpdatedConsumer(
            QueueRegister(mapOf()),
            objectMapper,
            failedQueueTaskService,
            transactionTemplate,
            lomOrderService,
            planFactService,
            eventService,
            orderLockService,
        )
    }

    @Test
    fun testConsumer() {
        doNothing().`when`(orderLockService).acquireLock(TEST_ORDER)
        whenever(lomOrderService.getWithEntitiesById(TEST_LOM_ID)).thenReturn(TEST_ORDER)

        consumer.processPayload(LomOrderCombinatorRouteUpdatedDto(lomOrderId = TEST_LOM_ID))

        verify(orderLockService).acquireLock(TEST_ORDER)
        verify(lomOrderService).getWithEntitiesById(TEST_LOM_ID)
        verify(planFactService).createPlanFactsForSegment(TEST_SEGMENT_1, SUPPORTED_STATUSES, SUPPORTED_PRODUCERS)
        verify(planFactService).createPlanFactsForSegment(TEST_SEGMENT_2, SUPPORTED_STATUSES, SUPPORTED_PRODUCERS)
    }

    companion object {
        var TEST_SEGMENT_1 = WaybillSegment(id = 1)
        var TEST_SEGMENT_2 = WaybillSegment(id = 2)
        const val TEST_LOM_ID = 1L
        val TEST_ORDER = joinInOrder(listOf(TEST_SEGMENT_1, TEST_SEGMENT_2)).apply {
            id = TEST_LOM_ID
            externalId = TEST_LOM_ID.toString()
        }
    }
}
