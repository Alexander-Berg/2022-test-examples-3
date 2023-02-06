package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.CourierLastMileRecipientPlanFactProcessor
import java.time.Instant
import java.time.LocalDate

internal class MkCourierDateAndDeliveryTypeAndPartnerSubtypeAndPartnerProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = MkCourierDateAndDeliveryTypeAndPartnerSubtypeAndPartnerProducer(converter)

    @Test
    @DisplayName("Проверка успешной применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Проверка применимости для план-факт процессора передачи на последнюю милю для курьерской доставки")
    fun isEligibleForCourierLastMile() {
        val planFact = preparePlanFact()
            .apply {
                producerName = CourierLastMileRecipientPlanFactProcessor::class.simpleName
            }
        assertSoftly {
            producer.isEligible(planFact) shouldBe true
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    @DisplayName("Проверка неуспешной применимости при подходящем SegmentType при неподходящем PartnerSubtype")
    fun isNotEligibleWhenWrongPartnerSubtype(partnerSubtype: PartnerSubtype) {
        val planFact = preparePlanFact(
            partnerSubtype = partnerSubtype
        )
        assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity в текущий день")
    fun produceEntityCurrentDay() {
        val planFact = preparePlanFact(
            instant = Instant.parse("2021-01-01T13:59:00.00Z")
        );
        val segment = planFact.entity as? WaybillSegment
        assertThat(
            producer.produceEntity(planFact)
        ).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(
                    planFact.expectedStatusDatetime,
                    DateTimeUtils.MOSCOW_ZONE
                ),
                partner = converter.toPartnerAggregationEntity(segment),
                partnerSubtype = segment?.partnerSubtype,
                deliveryType = segment?.order?.deliveryType
            )
        )
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity на следующий день")
    fun produceEntityNextDay() {
        val planFact = preparePlanFact(
            instant = Instant.parse("2021-01-01T17:00:00.00Z")
        );
        val segment = planFact.entity as? WaybillSegment
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(segment),
            partnerSubtype = segment?.partnerSubtype,
            deliveryType = segment?.order?.deliveryType
        )
    }


    private fun preparePlanFact(
        currentSegmentType: SegmentType = SegmentType.COURIER,
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_COURIER,
        instant: Instant = Instant.parse("2021-01-01T08:00:00.00Z")
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerId = 1L,
            segmentType = currentSegmentType,
            partnerSubtype = partnerSubtype
        )
        currentSegment.order = LomOrder(platformClientId = PlatformClient.BERU.id)

        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = instant,
            waybillSegmentType = SegmentType.POST,
        ).apply { entity = currentSegment }
    }
}
