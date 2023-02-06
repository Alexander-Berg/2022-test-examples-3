package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.entity.courier.CourierEventHistory
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.CourierStatusChangedDto
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.LogisticEventService
import ru.yandex.market.logistics.mqm.service.returns.CourierEventHistoryService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnBoxService

@ExtendWith(MockitoExtension::class)
class CourierStatusChangedConsumerTest {

    private lateinit var consumer: CourierStatusChangedConsumer

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    lateinit var transactionTemplate: TransactionOperations

    @Mock
    lateinit var planFactService: PlanFactService

    @Mock
    lateinit var logisticEventService: LogisticEventService

    @Mock
    lateinit var lrmReturnBoxService: LrmReturnBoxService

    @Mock
    lateinit var courierEventHistoryService: CourierEventHistoryService

    @BeforeEach
    private fun setUp() {
        consumer = CourierStatusChangedConsumer(
            QueueRegister(mapOf()),
            objectMapper,
            failedQueueTaskService,
            transactionTemplate,
            lrmReturnBoxService,
            logisticEventService,
            courierEventHistoryService
        )
    }

    @Test
    fun consumeSuccess() {
        whenever(lrmReturnBoxService.findByExternalId(eq("ext-box-id"))).thenReturn(LrmReturnBoxEntity(externalId = "ext-box-id"))
        whenever(courierEventHistoryService.existsByExternalBoxIdAndStatus(any(), any())).thenReturn(false)
        consumer.processPayload(
            CourierStatusChangedDto(
                CourierEventHistory(
                    externalBoxId = "ext-box-id",
                    status = CourierStatus.RECEIVED_PICKUP
                )
            )
        )
        verify(courierEventHistoryService).save(any())
        verify(logisticEventService).processCourierStatusChanged(any<LrmReturnBoxEntity>(), any())
    }

    @Test
    fun consumeRepeated() {
        whenever(lrmReturnBoxService.findByExternalId(eq("ext-box-id"))).thenReturn(LrmReturnBoxEntity(externalId = "ext-box-id"))
        whenever(courierEventHistoryService.existsByExternalBoxIdAndStatus(any(), any())).thenReturn(true)
        consumer.processPayload(
            CourierStatusChangedDto(
                CourierEventHistory(
                    externalBoxId = "ext-box-id",
                    status = CourierStatus.RECEIVED_PICKUP
                )
            )
        )
        verifyNoMoreInteractions(courierEventHistoryService)
        verifyZeroInteractions(logisticEventService)
    }
}
