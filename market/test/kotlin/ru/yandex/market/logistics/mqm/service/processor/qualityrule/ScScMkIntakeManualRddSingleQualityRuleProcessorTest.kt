package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute
import ru.yandex.market.logistics.lom.model.dto.RecalculateOrdersRouteRequest
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrdersRouteResponse
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrdersRouteResponse.RecalculationStatus
import ru.yandex.market.logistics.lom.model.enums.PointType
import ru.yandex.market.logistics.mqm.configuration.properties.ManualRouteRecalculationProperties
import ru.yandex.market.logistics.mqm.converter.lom.LomOrderCombinatorRouteConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.ManualRddRecalculationPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.ScScMkIntakeManualRddPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.TEST_ORDER_ID
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import ru.yandex.market.logistics.mqm.utils.getMkSegment
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment

@ExtendWith(MockitoExtension::class)
class ScScMkIntakeManualRddSingleQualityRuleProcessorTest {

    @Mock
    private lateinit var lomClient: LomClient

    private val properties = ManualRouteRecalculationProperties()
    private val routeConverter = LomOrderCombinatorRouteConverter()
    private val clock = TestableClock()
    private lateinit var processor: ScScMkIntakeManualRddSingleQualityRuleProcessor

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2022-04-08T11:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        properties.closePlanFactsIfDateNotChanged = true
        processor = ScScMkIntakeManualRddSingleQualityRuleProcessor(lomClient, routeConverter, clock, properties)
    }

    @Test
    @DisplayName("Вычисление нового маршрута и запись новой даты")
    fun successProcessing() {
        val planFact = preparePlanFact()
        mockLomClient()
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)!!
        assertSoftly {
            scheduleTime shouldBe null
            additionalData.newDeliveryDate shouldBe LocalDate.parse("2022-03-20")
            additionalData.newScMkIntakeTime shouldBe LocalDateTime.parse(("2022-03-20T02:00:00"))
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
        }
    }

    @Test
    @DisplayName("Вычисление нового маршрута и запись новой даты, нет времени приемки на СЦ МК")
    fun successProcessingWithoutScMkIntakeTime() {
        val planFact = preparePlanFact()
        mockLomClient(hasScMkIntakeTime = false)
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)!!
        assertSoftly {
            scheduleTime shouldBe null
            additionalData.newDeliveryDate shouldBe LocalDate.parse("2022-03-20")
            additionalData.newScMkIntakeTime shouldBe null
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
        }
    }

    @Test
    @DisplayName("Если новая дата не больше старой, то закрыть план-факт")
    fun closePlanFactIFNewDateNotGreaterThenOldDate() {
        val planFact = preparePlanFact()
        mockLomClient(newDeliveryDay = 19)
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)!!
        assertSoftly {
            scheduleTime shouldBe null
            additionalData.newDeliveryDate shouldBe LocalDate.parse("2022-03-19")
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @Test
    @DisplayName("Не закрывать план-факт если новая дата не больше старой и выключена настройка")
    fun doNotClosePlanFactIfDateNotChangedAndDisabledProperty() {
        properties.closePlanFactsIfDateNotChanged = false
        val planFact = preparePlanFact()
        mockLomClient(newDeliveryDay = 19)
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)!!
        assertSoftly {
            scheduleTime shouldBe null
            additionalData.newDeliveryDate shouldBe LocalDate.parse("2022-03-19")
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
        }
    }

    @Test
    @DisplayName("Если уже есть новая дата, то она не обновляется")
    fun doNotRecalculateSecondTime() {
        val planFact = preparePlanFact()
        planFact.setData(ManualRddRecalculationPlanFactAdditionalData(LocalDate.parse("2022-04-07")))
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)!!
        assertSoftly {
            scheduleTime shouldBe null
            additionalData.newDeliveryDate shouldBe LocalDate.parse("2022-04-07")
            verify(lomClient, never()).recalculateOrdersRoute(any())
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
        }
    }

    @Test
    @DisplayName("Не применять, если неправильный продьюсер")
    fun doNotRecalculateIfWrongProducer() {
        val planFact = preparePlanFact(producerName = "ScScIntakePlanFactProcessor")
        val scheduleTime = processor.process(QUALITY_RULE, planFact)

        val additionalData = planFact.getData(ManualRddRecalculationPlanFactAdditionalData::class.java)
        assertSoftly {
            scheduleTime shouldBe null
            additionalData shouldBe null
            verify(lomClient, never()).recalculateOrdersRoute(any())
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
        }
    }

    @Test
    @DisplayName("Бросить ошибку, если вернулся ответ с неправильным заказом")
    fun throwErrorIfWrongOrderId() {
        val planFact = preparePlanFact()
        mockLomClient(orderId = 321)
        val exception = assertThrows<IllegalStateException> { processor.process(QUALITY_RULE, planFact) }
        exception.message shouldStartWith "Lom returned incorrect order route recalculation"
    }

    @Test
    @DisplayName("Бросить ошибку, если не удалось пересчитать маршрут")
    fun throwErrorIfErrorDuringRouteRecalculation() {
        val planFact = preparePlanFact()
        mockLomClient(status = RecalculationStatus.ERROR)
        val exception = assertThrows<IllegalStateException> { processor.process(QUALITY_RULE, planFact) }
        exception.message shouldStartWith "Error during recalculating route"
    }

    @Test
    @DisplayName("Бросить ошибку, если новый маршрут null")
    fun throwErrorIfNewRouteIsNull() {
        val planFact = preparePlanFact()
        mockLomClient(routeIsNotNull = false)
        val exception = assertThrows<IllegalStateException> { processor.process(QUALITY_RULE, planFact) }
        exception.message shouldStartWith "New route is null "
    }

    private fun preparePlanFact(producerName: String = ScScMkIntakeManualRddPlanFactProcessor::class.simpleName!!)
        : PlanFact {
        val order = createMkOrder()
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = SCHEDULE_TIME,
            producerName = producerName,
        )
            .markCreated(clock.instant())
            .apply { entity = order.getMkSegment().getPreviousSegment() }
    }

    private fun mockLomClient(
        orderId: Long = TEST_ORDER_ID,
        status: RecalculationStatus = RecalculationStatus.SUCCESS,
        newDeliveryDay: Int = 20,
        routeIsNotNull: Boolean = true,
        hasScMkIntakeTime: Boolean = true,
    ) {
        val expectedRequest = RecalculateOrdersRouteRequest.builder().orderIds(listOf(TEST_ORDER_ID)).build()
        val combinatorRoute = if (routeIsNotNull) {
            buildCombinatorRoute(newDeliveryDay, hasScMkIntakeTime)
        } else {
            null
        }
        val recalculatedRouteDto = RecalculatedOrdersRouteResponse.RecalculatedRouteDto.builder()
            .orderId(orderId)
            .status(status)
            .newRoute(combinatorRoute)
            .build()
        val response = RecalculatedOrdersRouteResponse.builder()
            .recalculatedRouteDtoList(listOf(recalculatedRouteDto))
            .build()
        whenever(lomClient.recalculateOrdersRoute(eq(expectedRequest))).thenReturn(response)
    }

    private fun buildCombinatorRoute(newDeliveryDay: Int, hasScMkIntakeTime: Boolean) = CombinatorRoute().setRoute(
        CombinatorRoute.DeliveryRoute()
            .setDateTo(CombinatorRoute.Date().setYear(2022).setMonth(3).setDay(newDeliveryDay))
            .setPoints(if (hasScMkIntakeTime) buildPoints() else listOf())
    )

    private fun buildPoints() = listOf(
        CombinatorRoute.Point()
            .setIds(CombinatorRoute.PointIds().setPartnerId(1003L))
            .setSegmentType(PointType.WAREHOUSE)
            .setServices(
                listOf(
                    CombinatorRoute.DeliveryService().setStartTime(CombinatorRoute.Timestamp().setSeconds(1647730800))
                )
            )
    )

    companion object {
        private val SCHEDULE_TIME = Instant.parse("2022-04-07T10:00:00.00Z")
        private val QUALITY_RULE = QualityRule().apply {
            ruleProcessor = QualityRuleProcessorType.MANUAL_RDD_RECALCULATION
        }
    }
}
