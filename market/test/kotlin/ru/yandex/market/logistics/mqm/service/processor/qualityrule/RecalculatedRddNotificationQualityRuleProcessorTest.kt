package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto
import ru.yandex.market.logistics.lom.model.dto.PreDeliveryRecalculateRouteDatesRequestDto
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculatedRddNotificationPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.entity.lom.ChangeOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestReason
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestType
import ru.yandex.market.logistics.mqm.entity.rules.payloads.RecalculatedRddNotificationPayload
import ru.yandex.market.logistics.mqm.service.ChangeOrderRequestService
import ru.yandex.market.logistics.mqm.service.processor.planfact.order.OrderNotifyPrevDdPlanFactProcessor

@ExtendWith(MockitoExtension::class)
class RecalculatedRddNotificationQualityRuleProcessorTest: AbstractTest() {

    @Mock
    private lateinit var lomClient: LomClient

    @Mock
    private lateinit var changeOrderRequestService: ChangeOrderRequestService

    @InjectMocks
    private lateinit var processor: RecalculatedRddNotificationQualityRuleProcessor

    @Test
    @DisplayName("Может обработать правило")
    fun canProcess() {
        val qualityRule = mockQualityRule()
        processor.canProcess(qualityRule) shouldBe true
    }

    @Test
    @DisplayName("Успешный пересчет в день РДД")
    fun successOnRddDay() {
        val planFact = mockPlanFact()
        mockChangeOrderRequestService()
        mockLomClient()

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        val requestCaptor = argumentCaptor<PreDeliveryRecalculateRouteDatesRequestDto>()
        verify(lomClient).preDeliveryRddRecalculation(requestCaptor.capture())
        val requestDto = requestCaptor.firstValue
        assertSoftly {
            notificationData shouldNotBe null
            notificationData!!.wasNotified shouldBe true
            notificationData!!.changeOrderRequestId = 321
            requestDto.orderId shouldBe ORDER_ID
            requestDto.isRddDay shouldBe true
        }
    }

    @Test
    @DisplayName("Успешный пересчет за день до РДД")
    fun successBeforeRddDay() {
        val planFact = mockPlanFact(producerName = OrderNotifyPrevDdPlanFactProcessor::class.simpleName!!)
        mockChangeOrderRequestService()
        mockLomClient()

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        val requestCaptor = argumentCaptor<PreDeliveryRecalculateRouteDatesRequestDto>()
        verify(lomClient).preDeliveryRddRecalculation(requestCaptor.capture())
        val requestDto = requestCaptor.firstValue
        assertSoftly {
            notificationData shouldNotBe null
            notificationData!!.wasNotified shouldBe true
            notificationData!!.changeOrderRequestId = 321
            requestDto.orderId shouldBe ORDER_ID
            requestDto.isRddDay shouldBe false
        }
    }

    @Test
    @DisplayName("Не посылать запрос на пересчет РДД, если у ПФ неподходящий producerName")
    fun doNotRecalculateRddIfWrongProducerName() {
        val planFact = mockPlanFact(producerName = "BadProducerName")

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        verify(lomClient, never()).preDeliveryRddRecalculation(any())
        verify(changeOrderRequestService, never()).findByOrderIdWithTypes(any(), any())
        notificationData shouldBe null
    }

    @ParameterizedTest
    @EnumSource(
        value = PlanFactStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["ACTIVE"]
    )
    @DisplayName("Не посылать запрос на пересчет РДД, если у ПФ неподходящий статус")
    fun doNotRecalculateRddIfWrongPfStatus(planFactStatus: PlanFactStatus) {
        val planFact = mockPlanFact(planFactStatus = planFactStatus)

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        verify(lomClient, never()).preDeliveryRddRecalculation(any())
        verify(changeOrderRequestService, never()).findByOrderIdWithTypes(any(), any())
        notificationData shouldBe null
    }

    @Test
    @DisplayName("Не посылать запрос на пересчет РДД, если запрос уже посылался")
    fun doNotRecalculateRddIfAlreadyRecalculated() {
        val additionalData = RecalculatedRddNotificationPlanFactAdditionalData(true, 123)
        val planFact = mockPlanFact(additionalData = additionalData)

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        verify(lomClient, never()).preDeliveryRddRecalculation(any())
        verify(changeOrderRequestService, never()).findByOrderIdWithTypes(any(), any())
        assertSoftly {
            notificationData shouldNotBe null
            notificationData!!.wasNotified shouldBe true
        }
    }

    @Test
    @DisplayName("Не посылать запрос на пересчет РДД, если не было пересчетов рдд")
    fun doNotRecalculateRddIfPreviousCorIsAbsent() {
        val planFact = mockPlanFact()
        mockChangeOrderRequestService(previousCor = null)

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        verify(lomClient, never()).preDeliveryRddRecalculation(any())
        assertSoftly {
            notificationData shouldNotBe null
            notificationData!!.wasNotified shouldBe false
        }
    }

    @Test
    @DisplayName("Не посылать запрос на пересчет РДД, если последнее изменение рдд было из-за чекпоинта")
    fun doNotRecalculateRddIfPreviousCorTypeIsDeliveryDate() {
        val planFact = mockPlanFact()
        val previousCor = ChangeOrderRequest(requestType = ChangeOrderRequestType.DELIVERY_DATE)
        mockChangeOrderRequestService(previousCor = previousCor)

        processor.process(mockQualityRule(), planFact)

        val notificationData = planFact.getNotificationData()
        verify(lomClient, never()).preDeliveryRddRecalculation(any())
        assertSoftly {
            notificationData shouldNotBe null
            notificationData!!.wasNotified shouldBe false
        }
    }

    private fun mockQualityRule(): QualityRule {
        return QualityRule(
            enabled = true,
            expectedStatus = "UNKNOWN",
            ruleProcessor = QualityRuleProcessorType.RECALCULATED_RDD_NOTIFICATION,
            rule = RecalculatedRddNotificationPayload(
                enabledOnPddDay = true,
                enabledBeforePddDay = true
            )
        )
    }


    private fun mockPlanFact(
        producerName: String = ON_DD_PRODUCER_NAME,
        planFactStatus: PlanFactStatus = PlanFactStatus.ACTIVE,
        additionalData: RecalculatedRddNotificationPlanFactAdditionalData? = null,
    ): PlanFact {

        return PlanFact(
            producerName = producerName,
            expectedStatus = "UNKNOWN",
            planFactStatus = planFactStatus,
            entityId = ORDER_ID,
            entityType = EntityType.LOM_ORDER
        )
            .apply {
                if (additionalData != null) {
                    setData(additionalData)
                }
            }
    }

    private fun mockChangeOrderRequestService(previousCor: ChangeOrderRequest? = DEFAULT_PREVIOUS_COR) {
        val result = previousCor?.let { listOf(it) } ?: emptyList()
        whenever(changeOrderRequestService.findByOrderIdWithTypes(any(), any())).thenReturn(result)
    }

    private fun mockLomClient() {
        val dto = ChangeOrderRequestDto.builder().id(321).build()
        whenever(lomClient.preDeliveryRddRecalculation(any())).thenReturn(dto)
    }

    companion object {
        private const val ON_DD_PRODUCER_NAME = "OrderNotifyInDdPlanFactProcessor"
        private const val ORDER_ID = 12345L

        private val DEFAULT_PREVIOUS_COR = ChangeOrderRequest(
            id = 321,
            requestType = ChangeOrderRequestType.RECALCULATE_ROUTE_DATES,
            reason = ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION,
        )

        private fun PlanFact.getNotificationData(): RecalculatedRddNotificationPlanFactAdditionalData? {
            return getData(RecalculatedRddNotificationPlanFactAdditionalData::class.java)
        }
    }
}
