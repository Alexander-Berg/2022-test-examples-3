package ru.yandex.market.logistics.mqm.service.processor.planfact

import java.time.Instant
import java.time.ZoneOffset
import one.util.streamex.EntryStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PartnerService

@DisplayName("Абстрактный класс для тестирования создания план-фактов")
abstract class AbstractCreatedPlanFactProcessorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var partnerService: PartnerService

    @BeforeEach
    fun setup() {
        clock.setFixed(FIXED_TIME, ZoneOffset.UTC)
    }

    protected fun mockOrder(segments: MutableList<WaybillSegment>) {
        LomOrder().apply { waybill = segments }
    }

    protected fun mockWaybillSegment(partnerId: Long, indexInOrder: Int) =
        WaybillSegment().apply {
            this.waybillSegmentIndex = indexInOrder
            this.partnerId = partnerId
        }

    companion object {
        @JvmStatic
        protected val FIXED_TIME: Instant = Instant.parse("2021-01-01T00:00:00.00Z")

        @JvmStatic
        protected fun mockSegment(
            partnerType: PartnerType?,
            segmentType: SegmentType?,
            waybillSegmentIndex: Int
        ) =
            WaybillSegment().apply {
                this.id = 1L
                this.partnerId = 1L
                this.segmentType = segmentType
                this.partnerType = partnerType
                this.waybillSegmentIndex = waybillSegmentIndex
            }

        @JvmStatic
        protected fun mockSegmentWithOrderCreated(
            partnerType: PartnerType?,
            segmentType: SegmentType?,
            waybillSegmentIndex: Int
        ) = mockSegment(partnerType, segmentType, waybillSegmentIndex).apply { externalId = "ext1" }

        @JvmStatic
        protected fun ffScMvDs(partnerType: PartnerType?): LomOrder {
            val ff = mockSegment(partnerType, SegmentType.FULFILLMENT, 0)
            val sc = mockSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1)
            val mv = mockSegment(PartnerType.DELIVERY, SegmentType.MOVEMENT, 2)
            val ds = mockSegment(PartnerType.DELIVERY, SegmentType.COURIER, 3)
            return orderWithSegments(mutableListOf(ff, sc, mv, ds))
        }

        protected fun baseOrder() =
            LomOrder().apply {
                platformClientId = PlatformClient.BERU.id
                created = FIXED_TIME
            }

        @JvmStatic
        protected fun orderWithSegments(waybillSegments: MutableList<WaybillSegment>): LomOrder {
            val lomOrder = baseOrder().apply { id = 100 }
            EntryStream.of(waybillSegments).forKeyValue { index, segment ->
                segment.apply {
                    id = (index + 1).toLong()
                    order = lomOrder
                }
            }
            return lomOrder.apply { waybill = waybillSegments }
        }
    }
}
