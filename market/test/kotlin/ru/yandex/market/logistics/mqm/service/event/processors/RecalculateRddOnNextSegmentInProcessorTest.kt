package ru.yandex.market.logistics.mqm.service.event.processors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto
import ru.yandex.market.logistics.lom.model.dto.RecalculateRouteDatesRequestDto
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.RecalculateRddProcessingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculationRddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.utils.convertEnum
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class RecalculateRddOnNextSegmentInProcessorTest: AbstractTest() {

    @Mock
    lateinit var lomClient: LomClient

    @Mock
    lateinit var planFactService: PlanFactService

    lateinit var processor: RecalculateRddOnNextSegmentInProcessor

    @BeforeEach
    private fun setUp() {
        lenient().`when`(planFactService.getRddPlanFactProcessors()).thenReturn(setOf(RDD_PRODUCER))
        val properties = RecalculateRddProcessingProperties(
            onDelayRecalculationEnabled = true,
            onLateFactRecalculationEnabled = true,
            onNextInRecalculationEnabled = true
        )
        processor = RecalculateRddOnNextSegmentInProcessor(lomClient, planFactService, properties)
    }

    @DisplayName("Успешный сценарий пересчета")
    @Test
    fun success() {
        mockLomClient()
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(inCheckpoint.waybillSegment!!.getPreviousSegment())
        val context = mockContext(inCheckpoint, listOf(planFact))
        val requestCaptor = argumentCaptor<RecalculateRouteDatesRequestDto>()

        processor.waybillSegmentStatusAdded(context)

        val expectedRequestDto = RecalculateRouteDatesRequestDto.builder()
            .segmentId(inCheckpoint.waybillSegment!!.id)
            .startDateTime(OffsetDateTime.ofInstant(IN_DATE, DateTimeUtils.MOSCOW_ZONE))
            .segmentStatus(convertEnum<ru.yandex.market.logistics.lom.model.enums.SegmentStatus>(SegmentStatus.IN))
            .notifyUser(false)
            .build()
        verify(lomClient).recalculateRouteDates(requestCaptor.capture())
        assertSoftly {
            requestCaptor.firstValue shouldBe expectedRequestDto
            planFact.getRddData()!!.nextSegmentInInvocation shouldBe true
            planFact.getRddData()!!.corIdForNextSegmentIn shouldBe COR_ID
        }
    }

    @DisplayName("Не посылать запрос, если не IN checkpoint")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["IN"]
    )
    fun notRecalculateIfNotInCheckpoint(status: SegmentStatus) {
        val inCheckpoint = mockCheckpoint(status)
        val planFact = mockPlanFact(inCheckpoint.waybillSegment!!.getPreviousSegment())
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если нет предыдущего сегмента")
    @Test
    fun notRecalculateIfNoPreviousSegment() {
        val inCheckpoint = mockCheckpoint(hasPreviousSegment = false)
        val planFact = mockPlanFact(inCheckpoint.waybillSegment!!)
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если пф не от РДД процессора")
    @Test
    fun notRecalculateIfNotRddPf() {
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(inCheckpoint.waybillSegment!!.getPreviousSegment(), producerName = "Test")
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если пф не на out")
    @Test
    fun notRecalculateIfPfNotOut() {
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(
            waybillSegment = inCheckpoint.waybillSegment!!.getPreviousSegment(),
            expectedStatus = SegmentStatus.IN.name
        )
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если пф не на предыдущий сегмент")
    @Test
    fun notRecalculateIfPfNotOnPreviousSegment() {
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(waybillSegment = inCheckpoint.waybillSegment!!)
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если не посылался пересчет по просрочке")
    @Test
    fun notRecalculateIfNoDelayInvocation() {
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(
            waybillSegment = inCheckpoint.waybillSegment!!.getPreviousSegment(),
            delayInvocation = false
        )
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    @DisplayName("Не посылать запрос, если уже посылался пересчет по in")
    @Test
    fun notRecalculateIfWasNextInRecalculation() {
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(
            waybillSegment = inCheckpoint.waybillSegment!!.getPreviousSegment(),
            nextSegmentInInvocation = true
        )
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe true
    }

    @DisplayName("Не посылать запрос, если уже отключена настройка")
    @Test
    fun notRecalculateIfPropertyDisabled() {
        val properties = RecalculateRddProcessingProperties(
            onDelayRecalculationEnabled = true,
            onLateFactRecalculationEnabled = true,
            onNextInRecalculationEnabled = false
        )
        processor = RecalculateRddOnNextSegmentInProcessor(lomClient, planFactService, properties)
        val inCheckpoint = mockCheckpoint()
        val planFact = mockPlanFact(
            waybillSegment = inCheckpoint.waybillSegment!!.getPreviousSegment(),
        )
        val context = mockContext(inCheckpoint, listOf(planFact))

        processor.waybillSegmentStatusAdded(context)

        verify(lomClient, never()).recalculateRouteDates(any())
        planFact.getRddData()!!.nextSegmentInInvocation shouldBe false
    }

    private fun mockContext(
        checkpoint: WaybillSegmentStatusHistory,
        planFacts: List<PlanFact>
    ): LomWaybillStatusAddedContext {
        return LomWaybillStatusAddedContext(
            checkpoint = checkpoint,
            order = checkpoint.waybillSegment!!.order!!,
            orderPlanFacts = planFacts
        )
    }

    private fun mockCheckpoint(
        status: SegmentStatus = SegmentStatus.IN,
        hasPreviousSegment: Boolean = true,
    ): WaybillSegmentStatusHistory {
        val segments = mutableListOf<WaybillSegment>()
        if (hasPreviousSegment) {
            val firstSegment = WaybillSegment(id = 1, segmentType = SegmentType.FULFILLMENT)
            segments.add(firstSegment)
        }
        val secondSegment = WaybillSegment(id = 2)
        segments.add(secondSegment)
        val inCheckpoint = WaybillSegmentStatusHistory(status = status, date = IN_DATE)
            .apply { waybillSegment = secondSegment }
        secondSegment.waybillSegmentStatusHistory.add(inCheckpoint)

        joinInOrder(segments)
        return inCheckpoint
    }

    private fun mockPlanFact(
        waybillSegment: WaybillSegment,
        producerName: String = RDD_PRODUCER,
        expectedStatus: String = SegmentStatus.OUT.name,
        delayInvocation: Boolean = true,
        nextSegmentInInvocation: Boolean = false,
    ): PlanFact {
        val additionalData = RecalculationRddPlanFactAdditionalData(
            delayInvocation = delayInvocation,
            nextSegmentInInvocation = nextSegmentInInvocation,
        )
        return PlanFact(
            producerName = producerName,
            expectedStatusDatetime = DEFAULT_TIME,
            expectedStatus = expectedStatus,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            entityId = waybillSegment.id
        ).markCreated(DEFAULT_TIME)
            .apply {
                entity = waybillSegment
                setData(additionalData)
            }

    }

    private fun PlanFact.getRddData(): RecalculationRddPlanFactAdditionalData? =
        getData(RecalculationRddPlanFactAdditionalData::class.java)

    private fun mockLomClient() {
        val changeOrderRequestDto = ChangeOrderRequestDto.builder().id(COR_ID).build()
        whenever(lomClient.recalculateRouteDates(any())).thenReturn(changeOrderRequestDto)
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2022-01-27T11:00:00.00Z")
        private val IN_DATE = Instant.parse("2022-01-27T13:00:00.00Z")
        private const val RDD_PRODUCER = "TestRddProducer"
        private const val COR_ID = 321L
    }
}
