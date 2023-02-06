package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto
import ru.yandex.market.logistics.lom.model.dto.RecalculateRouteDatesRequestDto
import ru.yandex.market.logistics.mqm.configuration.properties.RecalculateRddProcessingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculationRddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.utils.convertEnum
import java.time.Instant
import java.time.OffsetDateTime


abstract class BaseRecalculateRddQualityRuleProcessorTest {

    protected val COR_ID = 321L

    @Mock
    protected lateinit var lomClient: LomClient

    protected fun createQualityRule() = QualityRule().apply { ruleProcessor = QualityRuleProcessorType.RECALCULATE_RDD }

    @Test
    @DisplayName("Процессор отправляет нотификацию в первый раз, если ПФ актуальный и не было нотификаций")
    fun successProcessingIfPlanFactActual() {
        val (existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId) = createActivePlanFactContext()
        mockLomClient()
        val processor = createProcessor(lomClient)
        val requestCaptor = argumentCaptor<RecalculateRouteDatesRequestDto>()

        val scheduleInstant = processor.process(createQualityRule(), existingPlanFact)

        verify(lomClient).recalculateRouteDates(requestCaptor.capture())
        val requestDto = requestCaptor.firstValue
        val expectedRequestDto = RecalculateRouteDatesRequestDto.builder()
            .segmentId(expectedSegmentId)
            .startDateTime(OffsetDateTime.ofInstant(expectedFactDatetime, DateTimeUtils.MOSCOW_ZONE))
            .segmentStatus(convertEnum<ru.yandex.market.logistics.lom.model.enums.SegmentStatus>(expectedSegmentStatus))
            .notifyUser(false)
            .build()
        val recalculationRddData = existingPlanFact.getRecalculationRddData()!!

        assertSoftly {
            scheduleInstant shouldBe null
            requestDto shouldBe expectedRequestDto
            recalculationRddData.delayInvocation shouldBe true
            recalculationRddData.corIdForDelay shouldBe COR_ID
            recalculationRddData.factInvocation shouldBe false
        }
    }

    @Test
    @DisplayName("Процессор отправляет нотификацию, если ПФ не актуальный и был факт")
    fun successProcessingIfPlanFactNotActualAndFact() {
        val (existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId) = createPlanFactNotActualAndFact()
        mockLomClient()
        val processor = createProcessor(lomClient)
        val requestCaptor = argumentCaptor<RecalculateRouteDatesRequestDto>()

        val scheduleInstant = processor.process(createQualityRule(), existingPlanFact)

        verify(lomClient).recalculateRouteDates(requestCaptor.capture())

        val requestDto = requestCaptor.firstValue
        val expectedRequestDto = RecalculateRouteDatesRequestDto.builder()
            .segmentId(expectedSegmentId)
            .startDateTime(OffsetDateTime.ofInstant(expectedFactDatetime, DateTimeUtils.MOSCOW_ZONE))
            .segmentStatus(convertEnum<ru.yandex.market.logistics.lom.model.enums.SegmentStatus>(expectedSegmentStatus))
            .notifyUser(false)
            .build()
        val recalculationRddData = existingPlanFact.getRecalculationRddData()!!

        assertSoftly {
            scheduleInstant shouldBe null
            requestDto shouldBe expectedRequestDto
            recalculationRddData.delayInvocation shouldBe true
            recalculationRddData.factInvocation shouldBe true
            recalculationRddData.corIdForFact shouldBe COR_ID
        }
    }

    @Test
    @DisplayName("Процессор не отправляет нотификацию, если уже были все вызовы")
    fun notSuccessProcessingIfPlanFactNotActualAndFact() {
        val (existingPlanFact, _, _, _) = createPlanFactNotActualAndFactAndAllInvocations()
        val processor = createProcessor(lomClient)

        val scheduleInstant = processor.process(createQualityRule(), existingPlanFact)

        scheduleInstant shouldBe null
        verify(lomClient, never()).recalculateRouteDates(any())
    }

    protected abstract fun createProcessor(
        lomClient: LomClient,
        properties: RecalculateRddProcessingProperties = RecalculateRddProcessingProperties(
            onDelayRecalculationEnabled = true,
            onLateFactRecalculationEnabled = true,
            onNextInRecalculationEnabled = true
        )
    ): BaseRecalculateRddQualityRuleProcessor

    protected abstract fun createActivePlanFactContext(): PlanFactContext

    protected abstract fun createPlanFactNotActualAndFact(): PlanFactContext

    protected abstract fun createPlanFactNotActualAndFactAndAllInvocations(): PlanFactContext

    protected fun PlanFact.getRecalculationRddData(): RecalculationRddPlanFactAdditionalData? {
        return getData(RecalculationRddPlanFactAdditionalData::class.java)
    }

    protected fun mockLomClient() {
        val changeOrderRequestDto = ChangeOrderRequestDto.builder().id(COR_ID).build()
        whenever(lomClient.recalculateRouteDates(any())).thenReturn(changeOrderRequestDto)
    }

    protected data class PlanFactContext(
        val existingPlanFact: PlanFact,
        val expectedFactDatetime: Instant,
        val expectedSegmentStatus: SegmentStatus,
        val expectedSegmentId: Long,
    )
}
