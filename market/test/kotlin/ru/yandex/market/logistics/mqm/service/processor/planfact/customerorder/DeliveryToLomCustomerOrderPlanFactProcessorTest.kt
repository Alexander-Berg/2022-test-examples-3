package ru.yandex.market.logistics.mqm.service.processor.planfact.customerorder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.configuration.properties.CustomerOrderPlanFactProcessingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrder
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderAcceptMethod
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderBuyer
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDelivery
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryPartnerType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEvent
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEventType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderItem
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderPaymentType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderRgb
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderStatus
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderSubstatus
import ru.yandex.market.logistics.mqm.service.customerorder.EXPECTED_STATUS
import ru.yandex.market.logistics.mqm.service.event.customerorder.CustomerOrderStatusOrSubstatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class DeliveryToLomCustomerOrderPlanFactProcessorTest {

    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    private lateinit var processor: DeliveryToLomCustomerOrderPlanFactProcessor

    private val processingProperties = CustomerOrderPlanFactProcessingProperties()

    @BeforeEach
    fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
        processor = DeliveryToLomCustomerOrderPlanFactProcessor(
            planFactService,
            processingProperties,
            clock
        )
    }

    @DisplayName("Успешное создание план-факта")
    @Test
    fun createPlanFactSuccessfully() {
        val planFactCaptor = argumentCaptor<List<PlanFact>>()
        val context = createCustomerOrderStatusChangedContext()
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService).save(planFactCaptor.capture())
        val planFact = planFactCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.CUSTOMER_ORDER
            planFact.entityId shouldBe BARCODE.toLong()
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    @DisplayName("Не создавать план-факт, если есть заказ в ломе")
    @Test
    fun doNotCreatePlanFactIfOrderInLom() {
        val context = createCustomerOrderStatusChangedContext(lomOrder = createMkOrder())
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если уже есть существующий")
    @Test
    fun doNotCreatePlanFactIfOneExists() {
        val context = createCustomerOrderStatusChangedContext(planFacts = listOf(createPlanFact()))
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если заказ DBS, но не в ПВЗ")
    @Test
    fun doNotCreatePlanFactForDbs() {
        val context = createCustomerOrderStatusChangedContext(
            orderAfter = createCustomerOrder(
                rgb = CustomerOrderRgb.WHITE,
                delivery = createCustomerOrderDelivery(deliveryType = CustomerOrderDeliveryType.PICKUP)
            )
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Cоздавать план-факт для DBS-ПВЗ заказа")
    @Test
    fun сreatePlanFactForDbsPvz() {
        val context = createCustomerOrderStatusChangedContext(
            orderAfter = createCustomerOrder(
                rgb = CustomerOrderRgb.WHITE,
                delivery = createCustomerOrderDelivery(
                    deliveryPartnerType = CustomerOrderDeliveryPartnerType.SHOP,
                    deliveryType = CustomerOrderDeliveryType.PICKUP
                )
            )
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService, only()).save(any())
    }

    @DisplayName("Проставить план-факт в IN_TIME по событию из лома")
    @Test
    fun setPlanFactStatusInTimeIfLomStatusChangedInTime() {
        val existingPlanFact = createPlanFact()
        val context = createLomOrderStatusChangedContext(planFacts = listOf(existingPlanFact))
        processor.lomOrderStatusChanged(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Проставить план-факт в EXPIRED по если пришел финальный статус из чекаутера")
    @Test
    fun setPlanFactStatusExpiredIfReceivedFinalStatusFromCheckouterInTime() {
        val existingPlanFact = createPlanFact()
        val orderAfter = createCustomerOrder(orderStatus = CustomerOrderStatus.CANCELLED)
        val orderBefore = createCustomerOrder(orderStatus = CustomerOrderStatus.RESERVED)
        val context = createCustomerOrderStatusChangedContext(
            orderBefore = orderBefore,
            orderAfter = orderAfter,
            planFacts = listOf(existingPlanFact)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("Создание план-факта в IN_TIME, если первым обрабатываем событие о финальном статусе в чекаутере")
    @Test
    fun createInTimePlanFactUponFinalStatusFromCheckouter() {
        val planFactCaptor = argumentCaptor<List<PlanFact>>()
        val context = createCustomerOrderStatusChangedContext(
            orderBefore = createCustomerOrder(orderStatus = CustomerOrderStatus.PROCESSING),
            orderAfter = createCustomerOrder(orderStatus = CustomerOrderStatus.CANCELLED)
        )
        processor.customerOrderStatusOrSubstatusChanged(context)
        verify(planFactService).save(planFactCaptor.capture())
        val planFact = planFactCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.CUSTOMER_ORDER
            planFact.entityId shouldBe BARCODE.toLong()
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe DEFAULT_TIME
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            planFact.scheduleTime shouldBe null
        }
    }

    private fun createCustomerOrderStatusChangedContext(
        orderBefore: CustomerOrder? = null,
        orderAfter: CustomerOrder = createCustomerOrder(),
        lomOrder: LomOrder? = null,
        planFacts: List<PlanFact> = listOf(),
    ): CustomerOrderStatusOrSubstatusChangedContext {
        val event = CustomerOrderHistoryEvent(
            eventType = CustomerOrderHistoryEventType.ORDER_STATUS_UPDATED,
            requestId = "123",
            orderBefore = orderBefore,
            orderAfter = orderAfter
        )
        return CustomerOrderStatusOrSubstatusChangedContext(
            event,
            lomOrder = lomOrder,
            planFacts
        )
    }

    private fun createCustomerOrder(
        orderStatus: CustomerOrderStatus = CustomerOrderStatus.PROCESSING,
        paymentType: CustomerOrderPaymentType = CustomerOrderPaymentType.PREPAID,
        rgb: CustomerOrderRgb = CustomerOrderRgb.BLUE,
        delivery: CustomerOrderDelivery = createCustomerOrderDelivery(),
    ) = CustomerOrder(
        id = BARCODE.toLong(),
        status = orderStatus,
        creationDate = CREATION_TIME,
        paymentType = paymentType,
        substatus = CustomerOrderSubstatus.STARTED,
        acceptMethod = CustomerOrderAcceptMethod.WEB_INTERFACE,
        rgb = rgb,
        delivery = delivery,
        changeRequests = listOf(),
        items = listOf(
            CustomerOrderItem(1L, 2L, 3L)
        ),
        fulfilment = false,
        buyer = CustomerOrderBuyer(0L)
    )

    private fun createCustomerOrderDelivery(
        deliveryPartnerType: CustomerOrderDeliveryPartnerType = CustomerOrderDeliveryPartnerType.YANDEX_MARKET,
        deliveryType: CustomerOrderDeliveryType = CustomerOrderDeliveryType.POST
    ) = CustomerOrderDelivery(
        marketBranded = true,
        deliveryPartnerType = deliveryPartnerType,
        type = deliveryType,
        deliveryServiceId = 0
    )

    private fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus = OrderStatus.DELIVERED,
        planFacts: List<PlanFact> = listOf()
    ): LomOrderStatusChangedContext {
        val order = createMkOrder()
        return LomOrderStatusChangedContext(
            order,
            orderStatus,
            planFacts
        )
    }

    private fun createPlanFact(): PlanFact {
        return PlanFact(
            producerName = DeliveryToLomCustomerOrderPlanFactProcessor::class.simpleName,
            entityType = EntityType.CUSTOMER_ORDER,
            entityId = BARCODE.toLong(),
            expectedStatusDatetime = EXPECTED_TIME
        ).markCreated(EXPECTED_TIME)
    }

    companion object {
        private const val BARCODE = "12345"
        private val DEFAULT_TIME = Instant.parse("2022-04-25T16:00:00.00Z")
        private val CREATION_TIME = Instant.parse("2022-04-25T15:50:00.00Z")
        private val EXPECTED_TIME = CREATION_TIME.plus(Duration.ofMinutes(60))
    }
}
