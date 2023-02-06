package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.MkMiddleMileRecipientPlanFactProcessor
import java.time.Instant
import java.time.LocalDate

internal class DateMkPartnerMiddleMileProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DateMkPartnerMiddleMileProducer(converter)

    @ParameterizedTest(name = AbstractTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка успешной применимости")
    fun isEligible(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
        assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Проверка применимости для план-факт процессора средней мили")
    fun isEligibleForMiddleMile() {
        val planFact = preparePlanFact()
            .apply {
                producerName = MkMiddleMileRecipientPlanFactProcessor::class.simpleName
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
        )
        val segment = planFact.entity as? WaybillSegment
        assertThat(
            producer.produceEntity(planFact)
        ).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(
                    planFact.expectedStatusDatetime,
                    DateTimeUtils.MOSCOW_ZONE
                ),
                partner = converter.toPartnerAggregationEntity(segment)
            )
        )
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity на следующий день")
    fun produceEntityNextDay() {
        val planFact = preparePlanFact(
            instant = Instant.parse("2021-01-01T17:00:00.00Z")
        )
        val segment = planFact.entity as? WaybillSegment
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(segment)
        )
    }

    private fun preparePlanFact(
        currentSegmentType: SegmentType = SegmentType.COURIER,
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_COURIER,
        instant: Instant = Instant.parse("2021-01-01T08:00:00.00Z"),
        platformClient: PlatformClient = PlatformClient.BERU,
    ) = PlanFact(
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDatetime = instant,
    ).apply {
        entity = WaybillSegment(
            partnerId = 1L,
            segmentType = currentSegmentType,
            partnerSubtype = partnerSubtype
        ).apply { order = LomOrder(platformClientId = platformClient.id) }
    }
}
