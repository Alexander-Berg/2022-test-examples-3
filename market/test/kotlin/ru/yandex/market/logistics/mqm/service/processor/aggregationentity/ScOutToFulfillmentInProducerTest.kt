package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.LrmPartnerAggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields
import ru.yandex.market.logistics.mqm.service.processor.planfact.lrm.AbstractLrmReturnPlanFactTest
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService

@ExtendWith(MockitoExtension::class)
class ScOutToFulfillmentInProducerTest : AbstractLrmReturnPlanFactTest() {

    @Mock
    lateinit var returnService: LrmReturnService

    lateinit var producer: ScOutToFulfillmentInProducer

    @BeforeEach
    fun initProducer() {
        producer = ScOutToFulfillmentInProducer()
    }

    @Test
    fun produce() {
        val planFact = createPlanFact()
        val aggregationData = producer.produceEntity(planFact)
        assertSoftly {
            aggregationData.lrmPartnerFrom shouldBe LrmPartnerAggregationEntity(
                id = 111,
                type = LogisticPointType.SORTING_CENTER
            )
            aggregationData.lrmPartnerTo shouldBe LrmPartnerAggregationEntity(
                id = 999,
                type = LogisticPointType.SORTING_CENTER
            )
        }
    }

    fun createPlanFact(): PlanFact {
        val planFact = PlanFact(expectedStatusDatetime = Instant.EPOCH)
        val segment = LrmReturnSegmentEntity(
            id = 666,
            externalBoxId = "box",
            lrmReturnId = 777,
            lrmSegmentId = 888,
            logisticPoint = LrmLogisticPointFields(partnerId = 999, type = LogisticPointType.SORTING_CENTER)
        )
        val nextSegment = LrmReturnSegmentEntity(
                shipment = LrmShipmentFields(
                        destination = LrmShipmentFields.Destination(
                                returnSegmentId = 888
                        )
                ),
                logisticPoint = LrmLogisticPointFields(
                        partnerId = 111,
                        type = LogisticPointType.SORTING_CENTER
                )
        )
        mockLrmReturn().withSegment(segment).withSegment(nextSegment)
        planFact.entity = segment
        return planFact
    }


}
