package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
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
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSource
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.LrmReturnCreatedDto
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.event.LogisticEventService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService

@ExtendWith(MockitoExtension::class)
class LrmReturnCreatedConsumerTest {

    private lateinit var consumer: LrmReturnCreatedConsumer

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    lateinit var transactionTemplate: TransactionOperations

    @Mock
    lateinit var returnService: LrmReturnService

    @Mock
    lateinit var logisticEventService: LogisticEventService

    @BeforeEach
    private fun setUp() {
        consumer = LrmReturnCreatedConsumer(
            QueueRegister(mapOf()),
            objectMapper,
            failedQueueTaskService,
            transactionTemplate,
            returnService,
            logisticEventService
        )
    }

    @Test
    fun consumeSuccess() {
        whenever(returnService.save(any())).thenReturn(LrmReturnEntity())
        consumer.processPayload(
            LrmReturnCreatedDto(
                entity = LrmReturnEntity(
                    lrmReturnId = 666,
                    source = ReturnSource.CLIENT,
                    orderExternalId = "aboba",
                    logisticPointFromId = 1
                )
            )
        )
        verify(returnService).findByLrmId(any())
        verify(returnService).save(any())
        verify(logisticEventService).processLrmReturnCreated(any())
    }

    @Test
    fun consumeSegmentExists() {
        whenever(returnService.findByLrmId(any())).thenReturn(LrmReturnEntity())
        consumer.processPayload(
            LrmReturnCreatedDto(
                LrmReturnEntity()
            )
        )
        verify(returnService).findByLrmId(any())
        verifyNoMoreInteractions(returnService)
        verifyZeroInteractions(logisticEventService)
    }
}

