package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.aggregationentity.PartnerAggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.service.processor.planfact.lrm.PvzOutToCourierReceivedPvzPlanFactProcessor
import ru.yandex.market.logistics.mqm.service.yt.YtDeliveryServiceForPvzService
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class LrmPvzOutToCourierInProducerTest {

    val converter = AggregationEntityConverter()

    @Mock
    lateinit var mcYtService: YtDeliveryServiceForPvzService

    lateinit var producer: LrmPvzOutToCourierInProducer

    @BeforeEach
    private fun setUp() {
        producer = LrmPvzOutToCourierInProducer(
            converter,
            mcYtService,
        )
    }

    @Test
    fun validAggregationEntity() {
        whenever(mcYtService.loadMcForPvz(eq(123))).thenReturn(1)
        producer.produceEntity(
            preparePlanFact()
        ) shouldBe AggregationEntity(
            date = LocalDate.of(2022, 4, 7),
            partner = PartnerAggregationEntity(
                id = 1,
            )
        )
    }

    private fun preparePlanFact(): PlanFact {
        return PlanFact(
            entityType = EntityType.LRM_RETURN_BOX,
            expectedStatusDatetime = Instant.parse("2022-04-07T10:00:00.00Z"),
            producerName = PvzOutToCourierReceivedPvzPlanFactProcessor::class.simpleName,
        )
            .apply {
                entity = LrmReturnBoxEntity(
                    externalId = "ext1",
                ).apply {
                    this.returnEntity = LrmReturnEntity().apply {
                        returnSegments = mutableSetOf(LrmReturnSegmentEntity(
                            logisticPoint = LrmLogisticPointFields(
                                logisticPointId = 123,
                                type = LogisticPointType.PICKUP
                            )
                        ))
                    }
                }
            }
    }
}
