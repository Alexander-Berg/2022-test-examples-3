package ru.yandex.market.abo.tms.cpa.order.status

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.core.exception.ExceptionalShopReason
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.shop.on.ShopOnService
import ru.yandex.market.abo.cpa.MbiApiService
import ru.yandex.market.abo.cpa.order.delivery.OrderDeliveryRepo
import ru.yandex.market.abo.cpa.order.service.CpaOrderStatRepo
import ru.yandex.market.abo.test.TestHelper
import ru.yandex.market.abo.util.db.toggle.DbToggleService
import ru.yandex.market.abo.util.kotlin.toLocalDate
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.checkout.checkouter.order.OrderStatus
import ru.yandex.market.core.abo.AboCutoff
import ru.yandex.market.core.cutoff.CutoffNotificationStatus
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus
import ru.yandex.market.mbi.api.client.entity.abo.CloseAboCutoffResponse
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffResponse
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

open class CpaOrderStatusCutoffUpdaterTest @Autowired constructor(
    private val cpaOrderStatRepo: CpaOrderStatRepo,
    private val orderDeliveryRepo: OrderDeliveryRepo,
    private val cutoffManager: CutoffManager,
    pgRoJdbcTemplate: JdbcTemplate,
    dbToggleService: DbToggleService,
    shopOnService: ShopOnService
) : EmptyTest() {
    private val mbiApiClient: MbiApiClient = mock()
    private val mbiOpenApiClient: MbiOpenApiClient = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val mbiApiService: MbiApiService = MbiApiService(
        mbiApiClient,
        mbiOpenApiClient,
        shopOnService,
        dbToggleService
    )
    private val cpaOrderStatusCutoffUpdater: CpaOrderStatusCutoffUpdater = CpaOrderStatusCutoffUpdater(
        pgRoJdbcTemplate, dbToggleService, cutoffManager, exceptionalShopsService, mbiApiService
    )

    @ParameterizedTest(name = "Test final status cutoff actualization for delivery type {0} and threshold {1} days")
    @CsvSource(value = [
        "POST,30",
        "PICKUP,14",
        "DELIVERY,2"
    ])
    fun `test final status cutoff actualization`(deliveryType: DeliveryType, thresholdDays: Long) {
        val hasCutoffAndShouldHave = TestHelper.TEST_SHOPS[0]
        val hasCutoffButShouldNot = TestHelper.TEST_SHOPS[1]
        val hasNoCutoffButShould = TestHelper.TEST_SHOPS[2]
        val hasNoCutoffAndShouldNot = TestHelper.TEST_SHOPS[3]
        val hasNoCutoffButExceptional = TestHelper.TEST_SHOPS[4]
        val hasNoCutoffButObsoleteOrders = TestHelper.TEST_SHOPS[6]

        var orderId = 1L
        createOrder(hasCutoffAndShouldHave, orderId++, getDaysAgo(179), getDaysAgo(thresholdDays+1), OrderStatus.PROCESSING, deliveryType)
        createOrder(hasCutoffButShouldNot, orderId++, getDaysAgo(179), getDaysAgo(thresholdDays-1), OrderStatus.PICKUP, deliveryType)
        createOrder(hasNoCutoffButShould, orderId++, getDaysAgo(179), getDaysAgo(thresholdDays+1), OrderStatus.DELIVERY, deliveryType)
        createOrder(hasNoCutoffAndShouldNot, orderId++, getDaysAgo(179), getDaysAgo(thresholdDays-1), OrderStatus.PROCESSING, deliveryType)
        createOrder(hasNoCutoffButExceptional, orderId++, getDaysAgo(179), getDaysAgo(thresholdDays+1), OrderStatus.PICKUP, deliveryType)
        createOrder(hasNoCutoffButObsoleteOrders, orderId, getDaysAgo(181), getDaysAgo(thresholdDays+1), OrderStatus.DELIVERY, deliveryType)

        whenever(exceptionalShopsService.loadShops(eq(ExceptionalShopReason.IGNORE_NOT_SET_FINAL_ORDER_STATUS)))
            .thenReturn(setOf(hasNoCutoffButExceptional))

        val shopsWithCutoffInMbi = listOf(
            hasCutoffAndShouldHave,
            hasCutoffButShouldNot
        )

        initMbiApiClientMock(shopsWithCutoffInMbi)

        cpaOrderStatusCutoffUpdater.updateCutoffs()

        val openCutoffShopIdCaptor = argumentCaptor<Long>()
        val openCutoffRequestCaptor = argumentCaptor<OpenAboCutoffRequest>()
        verify(mbiApiClient, times(1))
            .openAboCutoff(openCutoffShopIdCaptor.capture(), openCutoffRequestCaptor.capture())
        assertEquals(hasNoCutoffButShould, openCutoffShopIdCaptor.firstValue)
        assertEquals(
            "<abo-info><creation-date-threshold>"
                + getDaysAgo(180).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "</creation-date-threshold></abo-info>",
            openCutoffRequestCaptor.firstValue.aboInfo.trim().replace("\n", "")
        )

        val closeCutoffShopIdCaptor = argumentCaptor<Long>()
        verify(mbiApiClient, times(1)).closeAboCutoff(closeCutoffShopIdCaptor.capture(), any())
        assertEquals(hasCutoffButShouldNot, closeCutoffShopIdCaptor.firstValue)
    }

    @Test
    fun `test final status cutoff actualization with error`() {
        val shopId1 = TestHelper.TEST_SHOPS[0]
        val shopId2 = TestHelper.TEST_SHOPS[1]

        var orderId = 1L
        createOrder(shopId1, orderId++, getDaysAgo(179), getDaysAgo(31), OrderStatus.PICKUP, DeliveryType.POST)
        createOrder(shopId1, orderId++, getDaysAgo(179), getDaysAgo(31), OrderStatus.PROCESSING, DeliveryType.POST)
        createOrder(shopId2, orderId++, getDaysAgo(179), getDaysAgo(31), OrderStatus.PICKUP, DeliveryType.POST)
        createOrder(shopId2, orderId, getDaysAgo(179), getDaysAgo(31), OrderStatus.PROCESSING, DeliveryType.POST)

        whenever(exceptionalShopsService.loadShops(eq(ExceptionalShopReason.IGNORE_NOT_SET_FINAL_ORDER_STATUS)))
            .thenReturn(setOf())

        initMbiApiClientMockWithError()

        assertThrows<RuntimeException> { cpaOrderStatusCutoffUpdater.updateCutoffs() }

        val openCutoffShopIdCaptor = argumentCaptor<Long>()
        verify(mbiApiClient, times(2)).openAboCutoff(openCutoffShopIdCaptor.capture(), any())
    }

    private fun initMbiApiClientMock(actualCutoffShops: List<Long>) {
        whenever(mbiApiClient.getShopsWithAboCutoff(eq(AboCutoff.ORDER_FINAL_STATUS_NOT_SET), any()))
            .thenReturn(actualCutoffShops)
        whenever(mbiApiClient.openAboCutoff(any(), any()))
            .thenReturn(OpenAboCutoffResponse(0L, CutoffActionStatus.OK, CutoffNotificationStatus.SENT))
        whenever(mbiApiClient.closeAboCutoff(any(), any()))
            .thenReturn(CloseAboCutoffResponse(0L, CutoffActionStatus.OK, CutoffNotificationStatus.SENT))
        cutoffManager.setMbiApiService(mbiApiService)
    }

    private fun initMbiApiClientMockWithError() {
        whenever(mbiApiClient.getShopsWithAboCutoff(eq(AboCutoff.ORDER_FINAL_STATUS_NOT_SET), any()))
            .thenReturn(listOf())
        whenever(mbiApiClient.openAboCutoff(any(), any()))
            .thenReturn(OpenAboCutoffResponse(
                0L,
                CutoffActionStatus.ERROR,
                CutoffNotificationStatus.ERROR,
                RuntimeException()
            ))
        whenever(mbiApiClient.closeAboCutoff(any(), any()))
            .thenReturn(CloseAboCutoffResponse(
                0L,
                CutoffActionStatus.ERROR,
                CutoffNotificationStatus.ERROR,
                RuntimeException()))
        cutoffManager.setMbiApiService(mbiApiService)
    }

    private fun getDaysAgo(daysAgo: Long): Date {
        return Date.from(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
    }

    private fun createOrder(
        shopId: Long,
        orderId: Long,
        creationDate: Date,
        deliveryDate: Date,
        orderStatus: OrderStatus,
        deliveryType: DeliveryType
    ) {
        cpaOrderStatRepo.saveAndFlush(
            TestHelper.generateCpaOrderStat(orderId, shopId, Color.WHITE, creationDate, orderStatus, deliveryType)
        )

        orderDeliveryRepo.saveAndFlush(
            TestHelper.generateCpaOrderDelivery(orderId, deliveryDate, DeliveryPartnerType.SHOP)
        )
    }
}
