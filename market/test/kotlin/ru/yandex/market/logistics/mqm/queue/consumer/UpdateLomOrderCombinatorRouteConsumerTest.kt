package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute
import ru.yandex.market.logistics.mqm.configuration.properties.UpdateLomOrderCombinatorRouteConsumerProperties
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderCombinatorRouteUpdatedDto
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderIdNewCombinatorRouteDto
import ru.yandex.market.logistics.mqm.queue.producer.CombinatorRouteWasUpdatedProducer
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService

@ExtendWith(MockitoExtension::class)
class UpdateLomOrderCombinatorRouteConsumerTest {

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    lateinit var transactionTemplate: TransactionOperations

    @Mock
    lateinit var lomClient: LomClient

    @Mock
    lateinit var lomOrderService: LomOrderService

    @Mock
    lateinit var combinatorRouteEventProducer: CombinatorRouteWasUpdatedProducer

    lateinit var consumer: UpdateLomOrderCombinatorRouteConsumer

    @BeforeEach
    private fun setUp() {
        consumer = setupConsumer()
    }

    @Test
    fun testConsumer() {
        val testCombinatorRoute = mockCombinatorRoute()
        val routeWasUpdatedTask = LomOrderCombinatorRouteUpdatedDto(lomOrderId = TEST_LOM_ID)
        whenever(lomClient.getRouteByUuid(TEST_COMBINATOR_ID)).thenReturn(Optional.of(testCombinatorRoute))
        doNothing().whenever(lomOrderService).saveCombinatorRoute(TEST_LOM_ID, testCombinatorRoute)
        whenever(combinatorRouteEventProducer.produceTask(routeWasUpdatedTask)).thenReturn(1L)

        consumer.processPayload(LomOrderIdNewCombinatorRouteDto(TEST_LOM_ID, TEST_COMBINATOR_ID))

        verify(lomClient).getRouteByUuid(TEST_COMBINATOR_ID)
        verify(lomOrderService).saveCombinatorRoute(TEST_LOM_ID, testCombinatorRoute)
        verify(combinatorRouteEventProducer).produceTask(routeWasUpdatedTask)
    }

    @Test
    fun testConsumerDoNotTriggerNextTask() {
        val consumerWithoutNextTaskTrigger = setupConsumer(triggerRouteWasUpdatedTask = false)

        val testCombinatorRoute = mockCombinatorRoute()
        whenever(lomClient.getRouteByUuid(TEST_COMBINATOR_ID)).thenReturn(Optional.of(testCombinatorRoute))
        doNothing().whenever(lomOrderService).saveCombinatorRoute(TEST_LOM_ID, testCombinatorRoute)

        consumerWithoutNextTaskTrigger.processPayload(LomOrderIdNewCombinatorRouteDto(TEST_LOM_ID, TEST_COMBINATOR_ID))

        verify(lomClient).getRouteByUuid(TEST_COMBINATOR_ID)
        verify(lomOrderService).saveCombinatorRoute(TEST_LOM_ID, testCombinatorRoute)
        verifyZeroInteractions(combinatorRouteEventProducer)
    }

    @Test
    fun testConsumerDoNothingIfEmptyRoute() {
        whenever(lomClient.getRouteByUuid(TEST_COMBINATOR_ID)).thenReturn(Optional.empty())

        consumer.processPayload(LomOrderIdNewCombinatorRouteDto(TEST_LOM_ID, TEST_COMBINATOR_ID))

        verify(lomClient).getRouteByUuid(TEST_COMBINATOR_ID)
        verifyZeroInteractions(lomOrderService)
        verifyZeroInteractions(combinatorRouteEventProducer)
    }

    fun setupConsumer(triggerRouteWasUpdatedTask: Boolean = true) = UpdateLomOrderCombinatorRouteConsumer(
        QueueRegister(mapOf()),
        objectMapper,
        failedQueueTaskService,
        transactionTemplate,
        lomOrderService,
        lomClient,
        combinatorRouteEventProducer,
        UpdateLomOrderCombinatorRouteConsumerProperties(triggerRouteWasUpdatedTask = triggerRouteWasUpdatedTask),
    )

    fun mockCombinatorRoute() = CombinatorRoute().setRoute(
        CombinatorRoute.DeliveryRoute()
            .setCost(BigDecimal.ONE)
            .setTariffId(12L)
    )

    companion object {
        const val TEST_COMBINATOR_ID = "test_id"
        const val TEST_LOM_ID = 1L
    }
}
