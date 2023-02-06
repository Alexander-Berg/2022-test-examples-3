package ru.yandex.market.logistics.mqm.service.claim

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.converter.lom.LomSegmentStatusConverter
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Cost
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ShipmentType
import ru.yandex.market.logistics.mqm.repository.ClaimRepository
import ru.yandex.market.logistics.mqm.repository.ClaimUnitRepository
import ru.yandex.market.logistics.mqm.service.ClaimIdGenerator
import ru.yandex.market.logistics.mqm.service.ClaimServiceImpl
import ru.yandex.market.logistics.mqm.service.ClaimServiceImpl.Companion.AGREEMENT_DATE_STUB
import ru.yandex.market.logistics.mqm.service.ClaimServiceImpl.Companion.AGREEMENT_STUB
import ru.yandex.market.logistics.mqm.service.ClaimServiceImpl.Companion.CLAIM_MANAGER_STUB
import ru.yandex.market.logistics.mqm.service.ClaimStatusHistoryService
import ru.yandex.market.logistics.mqm.service.IssueLinkService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import ru.yandex.market.logistics.mqm.tms.claim.AbstractCreateClaimExecutor

@ExtendWith(MockitoExtension::class)
class ClaimServiceImplTest : AbstractTest() {

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var lomOrderService: LomOrderService

    @Mock
    private lateinit var lomWaybillSegmentStatusConverter: LomSegmentStatusConverter

    @Mock
    private lateinit var claimIdGenerator: ClaimIdGenerator

    @Mock
    private lateinit var issueLinkService: IssueLinkService

    @Mock
    private lateinit var claimRepo: ClaimRepository

    @Mock
    private lateinit var claimStatusHistoryService: ClaimStatusHistoryService

    @Mock
    private lateinit var claimUnitRepository: ClaimUnitRepository

    @Mock
    private lateinit var clock: TestableClock

    @Test
    @DisplayName("Дропшип заказ с 175 чекпоинтом на последнем СЦ не попадает в претензию, если ДШ самопривозный")
    fun skipPreparedToReturnOnLastScOrder() {
        val segmentIds = setOf(101L, 202L)
        val orderIds = listOf(1L, 2L)
        whenever(lomOrderService.findAllIdsBySegmentIds(segmentIds))
            .thenReturn(orderIds)
        whenever(lomOrderService.getWithEntitesByIds(orderIds.toSet()))
            .thenReturn(listOf(createValidOrder(), createPreparedToReturnOrder()))
        val claimService = ClaimServiceImpl(
            planFactService,
            lomOrderService,
            lomWaybillSegmentStatusConverter,
            claimIdGenerator,
            clock,
            claimRepo,
            issueLinkService,
            claimStatusHistoryService,
            claimUnitRepository
        )
        val validOrders = claimService.getValidOrders(segmentIds)
        assertSoftly {
            validOrders.size shouldBeExactly 1
            validOrders[0].id shouldBeExactly 1L
        }
    }

    @Test
    @DisplayName("Дропшим заказ с 180ым на чекпоинтом на последнем СЦ/ПВЗ/ФФ не попадает в претензию")
    fun skipReturnedOrder() {
        val segmentIds = setOf(303L)
        val orderIds = listOf(3L)
        whenever(lomOrderService.findAllIdsBySegmentIds(segmentIds))
            .thenReturn(orderIds)
        whenever(lomOrderService.getWithEntitesByIds(orderIds.toSet()))
            .thenReturn(listOf(createReturnedOrder()))
        val claimService = ClaimServiceImpl(
            planFactService,
            lomOrderService,
            lomWaybillSegmentStatusConverter,
            claimIdGenerator,
            clock,
            claimRepo,
            issueLinkService,
            claimStatusHistoryService,
            claimUnitRepository
        )
        val validOrders = claimService.getValidOrders(segmentIds)
        assertSoftly {
            validOrders.size shouldBeExactly 0
        }
    }

    @Test
    @DisplayName("Сбор данных для генерации pdf претензии")
    fun collectClaimData() {
        whenever(claimIdGenerator.generateClaimId()).thenReturn("test-id")
        whenever(clock.instant()).thenReturn(Instant.parse("2021-08-17T16:00:00.00Z"))
        whenever(lomWaybillSegmentStatusConverter.convertToCheckpointStatus(ApiVersion.DS, SegmentStatus.IN))
            .thenReturn(Optional.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START))
        val partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(1, "Партнер", null, null)
        val segment = createWaybillSegment()
        val claimService = ClaimServiceImpl(
            planFactService,
            lomOrderService,
            lomWaybillSegmentStatusConverter,
            claimIdGenerator,
            clock,
            claimRepo,
            issueLinkService,
            claimStatusHistoryService,
            claimUnitRepository
        )
        val claimData = claimService.collectClaimData(partnerInfo, listOf(segment))
        val order = claimData.orders[0]
        assertSoftly {
            claimData.id shouldBe "test-id"
            claimData.agreement shouldBe AGREEMENT_STUB
            claimData.agreementDate shouldBe AGREEMENT_DATE_STUB
            claimData.date shouldBe LocalDate.of(2021, 8, 17)
            claimData.amount.longValueExact() shouldBe 1234L
            claimData.manager shouldBe CLAIM_MANAGER_STUB
            claimData.contractorInfo.address shouldBe "Адрес неизвестен"
            claimData.contractorInfo.incorporation shouldBe "Партнер"
            order.externalId shouldBe "barcode"
            order.assessedValue.longValueExact() shouldBe 1234L
            order.comments shouldBe "10 октября 2021 г., заказ поступил на склад СД"
        }
    }

    private fun createValidOrder() = LomOrder(
        id = 1L,
        status = OrderStatus.PROCESSING
    ).apply {
        waybill = mutableListOf(
            WaybillSegment(
                id = 100L,
                partnerId = 172L,
                waybillSegmentIndex = 0,
                segmentType = SegmentType.FULFILLMENT,
                partnerType = PartnerType.FULFILLMENT
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.OUT)
                )
            },
            WaybillSegment(
                id = 101L,
                partnerId = 1006380L,
                waybillSegmentIndex = 1,
                segmentType = SegmentType.MOVEMENT,
                partnerType = PartnerType.DELIVERY
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.IN)
                )
            },
            WaybillSegment(
                id = 102L,
                partnerId = 62932L,
                waybillSegmentIndex = 2,
                segmentType = SegmentType.PICKUP,
                partnerType = PartnerType.DELIVERY
            )
        )
    }

    private fun createPreparedToReturnOrder() = LomOrder(
        id = 2L,
        status = OrderStatus.PROCESSING
    ).apply {
        waybill = mutableListOf(
            WaybillSegment(
                id = 201L,
                partnerId = 72651L,
                waybillSegmentIndex = 0,
                segmentType = SegmentType.FULFILLMENT,
                partnerType = PartnerType.DROPSHIP
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.OUT)
                )
                shipment = WaybillShipment(
                    type = ShipmentType.IMPORT
                )
            },
            WaybillSegment(
                id = 202L,
                partnerId = 1005634L,
                waybillSegmentIndex = 1,
                segmentType = SegmentType.SORTING_CENTER,
                partnerType = PartnerType.SORTING_CENTER
            ),
            WaybillSegment(
                id = 203L,
                partnerId = 101366L,
                waybillSegmentIndex = 2,
                segmentType = SegmentType.SORTING_CENTER,
                partnerType = PartnerType.SORTING_CENTER
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.RETURN_PREPARING_SENDER)
                )
            },
            WaybillSegment(
                id = 204L,
                partnerId = 1005492L,
                waybillSegmentIndex = 3,
                segmentType = SegmentType.COURIER,
                partnerType = PartnerType.DELIVERY
            ),
        )
    }

    private fun createReturnedOrder() = LomOrder(
        id = 3L,
        status = OrderStatus.PROCESSING
    ).apply {
        waybill = mutableListOf(
            WaybillSegment(
                id = 301L,
                partnerId = 76171L,
                waybillSegmentIndex = 0,
                segmentType = SegmentType.FULFILLMENT,
                partnerType = PartnerType.DROPSHIP
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.OUT)
                )
            },
            WaybillSegment(
                id = 302L,
                partnerId = 75735L,
                waybillSegmentIndex = 1,
                segmentType = SegmentType.SORTING_CENTER,
                partnerType = PartnerType.SORTING_CENTER
            ).apply {
                waybillSegmentStatusHistory = hashSetOf(
                    WaybillSegmentStatusHistory(status = SegmentStatus.RETURNED)
                )
            },
            WaybillSegment(
                id = 303L,
                partnerId = 83732L,
                waybillSegmentIndex = 2,
                segmentType = SegmentType.MOVEMENT,
                partnerType = PartnerType.DELIVERY
            ),
            WaybillSegment(
                id = 304L,
                partnerId = 93730L,
                waybillSegmentIndex = 3,
                segmentType = SegmentType.COURIER,
                partnerType = PartnerType.DELIVERY
            )
        )
    }

    private fun createWaybillSegment(): WaybillSegment {
        val segment = WaybillSegment().apply {
            waybillSegmentStatusHistory = hashSetOf(
                WaybillSegmentStatusHistory(
                    status = SegmentStatus.IN,
                    date = Instant.parse("2021-10-10T00:00:00.00Z")
                )
            )
        }
        LomOrder(
            barcode = "barcode",
            cost = Cost(BigDecimal.valueOf(1234L))
        ).apply {
            waybill = mutableListOf(segment)
        }
        return segment
    }
}
