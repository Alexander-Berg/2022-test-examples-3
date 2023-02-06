package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.service.geobase.GeoBaseClientService
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.ScScMkIntakeManualRddPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import ru.yandex.market.logistics.mqm.utils.getMkSegment
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment

@ExtendWith(MockitoExtension::class)
class ManualRddProducerTest {
    @Mock
    private lateinit var geoBaseClientService: GeoBaseClientService;

    private val converter = AggregationEntityConverter()
    private lateinit var producer: ManualRddProducer

    @BeforeEach
    private fun setUp() {
        producer = ManualRddProducer(converter, geoBaseClientService)
    }

    @Test
    fun isEligibleInternal() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка AggregationEntity")
    fun validAggregationEntity() {
        val planFact = preparePlanFact()
        val pfSegment = planFact.entity as WaybillSegment
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(pfSegment),
            partnerFrom = converter.toPartnerAggregationEntity(pfSegment.getPreviousSegment())
        )
    }

    private fun preparePlanFact(): PlanFact {
        val order = createMkOrder()
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = Instant.parse("2022-04-07T10:00:00.00Z"),
            producerName = ScScMkIntakeManualRddPlanFactProcessor::class.simpleName,
        )
            .apply { entity = order.getMkSegment().getPreviousSegment() }
    }
}
