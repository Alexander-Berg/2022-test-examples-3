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
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.CreateLrmSegmentDto
import ru.yandex.market.logistics.mqm.repository.LrmReturnSegmentRepository
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.LogisticEventService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService

@ExtendWith(MockitoExtension::class)
class CreateLrmSegmentConsumerTest {

    private lateinit var consumer: CreateLrmSegmentConsumer

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    lateinit var transactionTemplate: TransactionOperations

    @Mock
    lateinit var planFactService: PlanFactService

    @Mock
    lateinit var returnSegmentRepository: LrmReturnSegmentRepository

    @Mock
    lateinit var logisticEventService: LogisticEventService

    @Mock
    lateinit var returnService: LrmReturnService

    @BeforeEach
    private fun setUp() {
        consumer = CreateLrmSegmentConsumer(
            QueueRegister(mapOf()),
            objectMapper,
            failedQueueTaskService,
            transactionTemplate,
            returnSegmentRepository,
            logisticEventService,
            returnService,
        )
    }

    @Test
    fun consumeSuccess() {
        whenever(returnService.findByLrmId(any())).thenReturn(LrmReturnEntity())
        consumer.processPayload(
            CreateLrmSegmentDto(
                LrmReturnSegmentEntity()
            )
        )
        verify(returnSegmentRepository).findByLrmSegmentId(any())
        verify(returnService).save(any())
        verify(logisticEventService).processReturnSegmentCreated(any())
    }

    @Test
    fun consumeSegmentExists() {
        whenever(returnSegmentRepository.findByLrmSegmentId(any())).thenReturn(LrmReturnSegmentEntity())
        consumer.processPayload(
            CreateLrmSegmentDto(
                LrmReturnSegmentEntity()
            )
        )
        verify(returnSegmentRepository).findByLrmSegmentId(any())
        verifyNoMoreInteractions(returnSegmentRepository)
        verifyZeroInteractions(logisticEventService)
    }
}
