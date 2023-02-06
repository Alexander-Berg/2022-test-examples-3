package ru.yandex.market.logistics.mqm.service.processor.planfact.customerorder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrder
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderAcceptMethod
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDelivery
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryPartnerType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEvent
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEventType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderPaymentType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderRgb
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderStatus
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderSubstatus
import ru.yandex.market.logistics.mqm.service.customerorder.EXPECTED_STATUS
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderBuyer
import ru.yandex.market.logistics.mqm.service.event.customerorder.CustomerOrderStatusOrSubstatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder

@ExtendWith(MockitoExtension::class)
class DeliveredSyncCustomerOrderPlanFactProcessorTest {
    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    lateinit var processor: DeliveredSyncCustomerOrderPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = DeliveredSyncCustomerOrderPlanFactProcessor(
            planFactService,
            settingService,
            clock
        )
    }

    @DisplayName("Успешное создание план-факта")
    @Test
    fun createPlanFactSuccessfully() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactCaptor = argumentCaptor<List<PlanFact>>()
        val context = createLomOrderStatusChangedContext()
        processor.lomOrderStatusChanged(context)
        verify(planFactService).save(planFactCaptor.capture())
        val planFact = planFactCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.CUSTOMER_ORDER
            planFact.entityId shouldBe BARCODE.toLong()
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe Instant.parse("2022-01-26T16:00:00.00Z")
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2022-01-26T16:00:00.00Z")
        }
    }

    @DisplayName("Не создавать план-факт, если новый статус заказа в LOM не DELIVERED")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERED"]
    )
    fun doNotCreatePlanFactIfNewLomStatusIsNotDelivered(orderStatus: OrderStatus) {
        val context = createLomOrderStatusChangedContext(orderStatus = orderStatus)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Проставить план-факт в NOT_ACTUAL, если новый статус заказа DELIVERED пришел после плана")
    @Test
    fun setPlanFactNotActualIfNewCheckouterStatusIsDeliveredAfterPlan() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = DeliveredSyncCustomerOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2022-01-24T16:00:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = createCustomerOrderStatusChangedContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
            existingPlanFact.endOfProcessingDatetime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставить план-факт в IN_TIME, если новый статус заказа DELIVERED пришел до плана")
    @Test
    fun setPlanFactExpiredIfNewCheckouterStatusIsDeliveredBeforePlan() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = DeliveredSyncCustomerOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2022-01-26T16:00:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = createCustomerOrderStatusChangedContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            existingPlanFact.scheduleTime shouldBe null
        }
    }

    @DisplayName("Не создавать план-факт IN_TIME, если новый статус заказа в Checkouter не DELIVERED или CANCELLED")
    @ParameterizedTest
    @EnumSource(
        value = CustomerOrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERED", "CANCELLED"]
    )
    fun doNotCreatePlanFactIfNewCheckouterStatusIsNotDelivered(orderStatus: CustomerOrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val context = createCustomerOrderStatusChangedContext(
            orderStatus = orderStatus
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Успешно создать IN_TIME план-факт по событию от Checkouter, если план-факта еще не существует")
    @ParameterizedTest
    @EnumSource(
        value = CustomerOrderStatus::class,
        names = ["DELIVERED", "CANCELLED"]
    )
    fun createInTimePlanFactSuccessfully(orderStatus: CustomerOrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactCaptor = argumentCaptor<List<PlanFact>>()
        val context = createCustomerOrderStatusChangedContext(orderStatus = orderStatus)
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService).save(planFactCaptor.capture())
        val planFact = planFactCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.CUSTOMER_ORDER
            planFact.entityId shouldBe BARCODE.toLong()
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.factStatusDatetime shouldBe DEFAULT_TIME
            planFact.expectedStatusDatetime shouldBe DEFAULT_TIME
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            planFact.scheduleTime shouldBe null
        }
    }

    @DisplayName("Не создавать план-факт по событию из LOM, если по такому заказу п-ф уже существует")
    @Test
    fun doNotCreatePlanFactIfItAlreadyExists() {
        val existingPlanFact = PlanFact(
            producerName = DeliveredSyncCustomerOrderPlanFactProcessor::class.simpleName,
            entityType = EntityType.CUSTOMER_ORDER,
            entityId = ORDER_ID,
            expectedStatusDatetime = DEFAULT_TIME
        ).markInTime()
        val context = createLomOrderStatusChangedContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Проставить план-факт в EXPIRED по событию из чекаутера о статусе CANCELLED до плана")
    @Test
    fun setPlanFactExpiredUponCancelledStatusFromCheckouter() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = DeliveredSyncCustomerOrderPlanFactProcessor::class.simpleName,
            entityType = EntityType.CUSTOMER_ORDER,
            entityId = ORDER_ID,
            expectedStatusDatetime = DEFAULT_TIME.plusSeconds(10)
        ).markCreated(DEFAULT_TIME)
        val context = createCustomerOrderStatusChangedContext(
            orderStatus = CustomerOrderStatus.CANCELLED,
            planFacts = listOf(existingPlanFact)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Проставить план-факт в NOT_ACTUAL по событию из чекаутера о статусе CANCELLED после плана")
    @Test
    fun setPlanFactNotActualUponCancelledStatusFromCheckouter() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME.plusSeconds(10))
        val existingPlanFact = PlanFact(
            producerName = DeliveredSyncCustomerOrderPlanFactProcessor::class.simpleName,
            entityType = EntityType.CUSTOMER_ORDER,
            entityId = ORDER_ID,
            expectedStatusDatetime = DEFAULT_TIME
        ).markCreated(DEFAULT_TIME)
        val context = createCustomerOrderStatusChangedContext(
            orderStatus = CustomerOrderStatus.CANCELLED,
            planFacts = listOf(existingPlanFact)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    private fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus = OrderStatus.DELIVERED,
        planFacts: List<PlanFact> = listOf()
    ): LomOrderStatusChangedContext {
        val segment = WaybillSegment()
            .apply {
                waybillSegmentStatusHistory = mutableSetOf(
                    WaybillSegmentStatusHistory(
                        status = SegmentStatus.OUT,
                        date = Instant.parse("2022-01-25T16:00:00.00Z")
                    )
                )
            }
        val order = joinInOrder(listOf(segment))
            .apply {
                id = ORDER_ID
                barcode = BARCODE
            }
        return LomOrderStatusChangedContext(
            order,
            orderStatus,
            planFacts
        )
    }

    private fun createCustomerOrderStatusChangedContext(
        orderStatus: CustomerOrderStatus = CustomerOrderStatus.DELIVERED,
        planFacts: List<PlanFact> = listOf()
    ): CustomerOrderStatusOrSubstatusChangedContext {
        val order = CustomerOrder(
            id = BARCODE.toLong(),
            status = orderStatus,
            creationDate = clock.instant(),
            paymentType = CustomerOrderPaymentType.PREPAID,
            substatus = CustomerOrderSubstatus.STARTED,
            acceptMethod = CustomerOrderAcceptMethod.WEB_INTERFACE,
            rgb = CustomerOrderRgb.BLUE,
            delivery = CustomerOrderDelivery(
                marketBranded = true,
                deliveryPartnerType = CustomerOrderDeliveryPartnerType.YANDEX_MARKET,
                type = CustomerOrderDeliveryType.POST,
                deliveryServiceId = 0
            ),
            changeRequests = listOf(),
            items = listOf(),
            fulfilment = false,
            buyer = CustomerOrderBuyer(0L)
        )
        val event = CustomerOrderHistoryEvent(
            eventType = CustomerOrderHistoryEventType.ORDER_CANCELLATION_REQUESTED,
            requestId = "",
            orderBefore = null,
            orderAfter = order
        )
        return CustomerOrderStatusOrSubstatusChangedContext(
            event,
            lomOrder = null,
            planFacts
        )
    }

    companion object {
        private const val BARCODE = "12345"
        private const val ORDER_ID = 123L
        private val DEFAULT_TIME = Instant.parse("2022-01-25T16:00:00.00Z")
    }
}
