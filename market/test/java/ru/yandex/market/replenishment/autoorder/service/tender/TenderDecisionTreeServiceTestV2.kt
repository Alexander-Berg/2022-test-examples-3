package ru.yandex.market.replenishment.autoorder.service.tender

import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.application.properties.utils.Environments.PRESTABLE
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mboc.http.DeliveryParams
import ru.yandex.market.mboc.http.MboMappingsForDelivery
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
import ru.yandex.market.mboc.http.MbocMatrixAvailability
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.ShopSkuKeyWithStatus
import ru.yandex.market.replenishment.autoorder.model.SskuStatus
import ru.yandex.market.replenishment.autoorder.model.TenderStatus
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Environment.TENDER_NEW_DPR
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.service.MailSenderService
import ru.yandex.market.replenishment.autoorder.service.client.DeepMindClient
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService
import java.time.LocalDateTime

@WithMockLogin
class TenderDecisionTreeServiceTestV2 : FunctionalTest() {
    companion object {
        private val MOCKED_DATE_TIME = LocalDateTime.of(2020, 9, 6, 8, 0)
        private val MOCKED_SSKUS_STATUSES = listOf(
            ShopSkuKeyWithStatus(4201, "234234", SskuStatus.DELISTED),
            ShopSkuKeyWithStatus(4201, "235235", SskuStatus.INACTIVE_TMP),
            ShopSkuKeyWithStatus(4201, "234234_2", SskuStatus.ACTIVE),
            ShopSkuKeyWithStatus(4202, "234234", SskuStatus.ACTIVE)
        )

        init {
            // чтобы тесты не ломались после REPLENISHMENT-7979
            System.setProperty("environment", PRESTABLE)
        }
    }

    private lateinit var emailService: MailSenderService

    @Autowired
    private lateinit var tenderService: TenderService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var environmentService: EnvironmentService = Mockito.mock(EnvironmentService::class.java)
    private var deepMindClient: DeepMindClient = Mockito.mock(DeepMindClient::class.java)

    @Before
    fun mockDateTime() {
        setTestTime(MOCKED_DATE_TIME)
    }

    @Before
    fun mockServices() {
        `when`(deepMindClient.getSskusStatuses(any())).thenReturn(MOCKED_SSKUS_STATUSES)
        `when`(deepMindClient.updateSskusStatuses(any())).thenReturn(true)
        emailService = Mockito.mock(MailSenderService::class.java)
        `when`(environmentService.getBooleanWithDefault(TENDER_NEW_DPR, false)).thenReturn(true)
        mockEnvironmentService()
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest.before.csv"],
        after = ["calculateTenderTest.after.csv"]
    )
    fun testCalculateTender() {
        mockOpenWarehouses()
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_withNegativeFactors.before.csv"],
        after = ["calculateTenderTest_withNegativeFactors.after.csv"]
    )
    fun testCalculateTenderWithNegativeFactors() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_withDelayDays.before.csv"],
        after = ["calculateTenderTest_withDelayDays.after.csv"]
    )
    fun testCalculateTenderWithDelayDays() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_withoutTenderDecisionTreeProps.before.csv"]
    )
    fun testCalculateTenderWithoutTenderDecisionTreeProps_isWrong() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        val error = assertThrows<java.lang.IllegalStateException> { tenderService.calculateTender(1) }
        assertEquals("Not found tender tree props for tender id 1", error.message)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest.before.csv"],
        after = ["calculateTenderTest.after.csv"]
    )
    fun testCalculateTenderWontChageIfUsdIsChanged() {
        mockOpenWarehouses()
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)

        // change rate
        jdbcTemplate.update("update currency_rate set rate = 1")

        // one more run
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["testCalculateSkippingDeadstock.before.csv"],
        after = ["testCalculateSkippingDeadstock.after.csv"]
    )
    fun testCalculateSkippingDeadstock() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateWithAutoAssignWarehouses.before.csv"],
        after = ["calculateWithAutoAssignWarehouses.after.csv"]
    )
    fun testCalculateWithAutoAssignWarehouses() {
        mockOpenWarehouses(
            OpenWarehouse("1", 2),
            OpenWarehouse("1", 3, listOf(172))
        )
        `when`(deepMindClient.getSskusStatuses(any())).thenReturn(
            listOf(
                ShopSkuKeyWithStatus(2, "1", SskuStatus.ACTIVE),
                ShopSkuKeyWithStatus(3, "1", SskuStatus.ACTIVE),
            )
        )
        mockDeepMindClient()

        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_withUE.before.csv"],
        after = ["calculateTenderTest_withUE.after.csv"]
    )
    fun testCalculateTenderWithUE() {
        mockOpenWarehouses()
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_increaseToMinAmount.before.csv"],
        after = ["calculateTenderTest_increaseToMinAmount.after.csv"]
    )
    fun testCalculateTenderIncreaseToMinAmount() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_skipIncreaseToMinAmountForNotDirect.before.csv"],
        after = ["calculateTenderTest_skipIncreaseToMinAmountForNotDirect.after.csv"]
    )
    fun testCalculate_skipIncreaseToMinAmountForNotDirect() {
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    @Test
    @DbUnitDataSet(
        before = ["calculateTenderTest_withGrossMargin.before.csv"],
        after = ["calculateTenderTest_withGrossMargin.after.csv"]
    )
    fun testCalculateTenderWithGrossMargin() {
        mockOpenWarehouses()
        tenderService.setStatus(1, TenderStatus.OFFERS_COLLECTED)
        tenderService.calculateTender(1)
    }

    private fun mockDeepMindClient() {
        ReflectionTestUtils.setField(tenderService, "deepMindClient", deepMindClient)
    }

    private fun mockEnvironmentService() {
        ReflectionTestUtils.setField(tenderService, "environmentService", environmentService)
    }

    private data class OpenWarehouse(val ssku: String, val supplierId: Int, val lockedWarehouses: List<Long> = listOf())

    private fun mockOpenWarehouses(vararg warehouses: OpenWarehouse) {
        val deliveryParams = Mockito.mock(DeliveryParams::class.java)
        val response = SearchFulfilmentSskuParamsResponse.newBuilder()
        warehouses.forEach {
            val info = MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setAvailability(SupplierOffer.Availability.ACTIVE)
                .setShopSku(it.ssku)
                .setSupplierId(it.supplierId)
            it.lockedWarehouses.forEach { w ->
                info.addWarehouseIntervalAvailabilities(
                    MbocMatrixAvailability.WarehouseIntervalAvailability.newBuilder().setWarehouseId(w).build()
                )
            }
            response.addFulfilmentInfo(info.build())
        }
        `when`(deliveryParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(response.build())
        ReflectionTestUtils.setField(tenderService, "deliveryParams", deliveryParams)
    }
}
