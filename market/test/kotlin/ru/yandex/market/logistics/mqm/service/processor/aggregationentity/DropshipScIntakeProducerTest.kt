package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DropshipScIntakeProducerTest {
    private val converter = AggregationEntityConverter()
    private val producer = DropshipScIntakeProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Не агрегируется если план-факт не для сегмента")
    fun isNonEligibleIfNotLomSegment(entityType: EntityType) {
        val planFact = PlanFact(entityType = entityType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Не агрегируется если текущий сегмент дропофф")
    fun isNonEligibleIfWrongCurrentPartnerType() {
        val planFact = preparePlanFact(isDropoff = true)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DROPSHIP"]
    )
    @DisplayName("Не агрегируется если неправильный тип партнера на предыдущем сегменте")
    fun isNonEligibleIfWrongPreviousPartnerType(previousType: PartnerType) {
        val planFact = preparePlanFact(previousType = previousType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Не агрегируется если неправильный тип партнера на текущем сегменте")
    fun isNonEligibleIfWrongCurrentPartnerType(currentType: PartnerType) {
        val planFact = preparePlanFact(currentType = currentType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity для текущего дня")
    fun produceEntity() {
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T15:59:00.00Z")
        }
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partnerFrom = converter.toPartnerAggregationEntity(previousSegment)
        )
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity для следующего дня")
    fun produceEntityNextDay() {
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T16:01:00.00Z")
        }
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partnerFrom = converter.toPartnerAggregationEntity(previousSegment)
        )
    }

    private fun preparePlanFact(
        previousType: PartnerType = PartnerType.DROPSHIP,
        currentType: PartnerType = PartnerType.SORTING_CENTER,
        isDropoff: Boolean = false
    ): PlanFact {
        val previousSegment = WaybillSegment(partnerType = previousType)
        val currentSegment = WaybillSegment(partnerId = 1, externalId = "123", partnerType = currentType)
        joinInOrder(listOf(previousSegment, currentSegment))

        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT).apply { entity = currentSegment }
    }
}
