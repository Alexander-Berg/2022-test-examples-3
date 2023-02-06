package ru.yandex.market.wms.inbound_management.service

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.arrayContainingInAnyOrder
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestConstructor
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.Receipt
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptStatusHistory
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptServicesDao
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptStatusHistoryDao
import ru.yandex.market.wms.common.util.isZero
import ru.yandex.market.wms.inbound_management.dao.ReceiptToPriorityDao
import ru.yandex.market.wms.inbound_management.dao.SkuToOosDao
import ru.yandex.market.wms.inbound_management.service.CalculatePriorityService.PriorityCalcData
import ru.yandex.market.wms.inbound_management.service.CalculatePriorityService.ReceiptPriorities
import ru.yandex.market.wms.shared.libs.business.logger.BusinessLogger
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalculatePriorityServiceTest(
    private val receiptDao: ReceiptDao,
    private val receiptStatusHistoryDao: ReceiptStatusHistoryDao,
    private val receiptDetailDao: ReceiptDetailDao,
    private val receiptToPriorityDao: ReceiptToPriorityDao,
    private val receiptServicesDao: ReceiptServicesDao,
    private val configService: DbConfigService,
    private val skuToOosDao: SkuToOosDao,
    private val lotDao: LotDao,
    private val businessLogger: BusinessLogger,
) : IntegrationTest() {

    private lateinit var priorityService: CalculatePriorityService

    @BeforeEach
    fun initMocks() {
        priorityService = CalculatePriorityService(
            receiptDao, receiptStatusHistoryDao, receiptToPriorityDao, configService, receiptDetailDao,
            receiptServicesDao, skuToOosDao, lotDao, Clock.fixed(Instant.parse("2022-04-19T09:00:00Z"), UTC),
            businessLogger
        )
    }

    companion object {
        private const val RECEIPT_1_KEY = "0000000101"
        private const val RECEIPT_2_KEY = "0000001612"
        private const val RECEIPT_3_KEY = "0000000000"
        private val receipt1: Receipt = Receipt.builder()
            .receiptKey(RECEIPT_1_KEY)
            .build()
        private val receipt2 = Receipt.builder()
            .receiptKey(RECEIPT_2_KEY)
            .build()
        private val receiptPriority1 = PriorityCalcData(
            receiptKey = receipt1.receiptKey,
            receiptTypeCoeff = BigDecimal.ZERO,
            supplierBonusCoeff = BigDecimal.ZERO,
            oos = BigDecimal.valueOf(2.5007),
            duration = -10,
            sla = 24,
            timeIntervalCoeff = BigDecimal.ONE,
            receiptCapacity = BigDecimal.valueOf(19091, 4),
            rateCoeff = BigDecimal.valueOf(0, 4),
            qtyExpected = BigDecimal.valueOf(42000000, 5),
            palletAcceptanceStartedAt = Instant.parse("2022-04-18T18:20:33Z"),
            oosPosition = BigDecimal.valueOf(10000, 4),
            fifoPosition = BigDecimal.valueOf(0, 4)
        )

        private val receiptPriority2 = PriorityCalcData(
            receiptKey = receipt2.receiptKey,
            receiptTypeCoeff = BigDecimal.ZERO,
            supplierBonusCoeff = BigDecimal.valueOf(100),
            oos = BigDecimal.valueOf(0.3228),
            duration = -5,
            sla = 24,
            timeIntervalCoeff = BigDecimal.ONE,
            receiptCapacity = BigDecimal.valueOf(36910, 4),
            rateCoeff = BigDecimal.valueOf(10000, 4),
            qtyExpected = BigDecimal.valueOf(81200000, 5),
            palletAcceptanceStartedAt = Instant.parse("2022-04-18T14:00:00Z"),
            oosPosition = BigDecimal.valueOf(0, 4),
            fifoPosition = BigDecimal.valueOf(10000, 4)
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/service/before.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/service/sku_oos.xml", type = DatabaseOperation.INSERT)
    )
    fun prepareCalcData() {
        val prepareCalcData = priorityService.prepareCalcData()
        assertThat(
            prepareCalcData.toTypedArray(),
            arrayContainingInAnyOrder(receiptPriority1, receiptPriority2)
        )
    }

    @Test
    @DatabaseSetup("/service/empty-db.xml")
    fun calcWithNoData() {
        val prepareCalcData = priorityService.prepareCalcData()
        assertTrue(prepareCalcData.isEmpty())
    }

    @Test
    @DatabaseSetup("/service/before.xml")
    fun receiptsToPriorityPositions() {
        assertThat(
            priorityService.receiptsToPriorityPositions(listOf(receiptPriority1, receiptPriority2)),
            containsInAnyOrder(
                ReceiptPriorities(
                    receiptKey = RECEIPT_1_KEY,
                    priorityCoeff = BigDecimal.valueOf(20000, 4),
                    positionByPriority = 2
                ),
                ReceiptPriorities(
                    receiptKey = RECEIPT_2_KEY,
                    priorityCoeff = BigDecimal.valueOf(1030000, 4),
                    positionByPriority = 1
                )
            )
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/service/before.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/service/sku_oos.xml", type = DatabaseOperation.INSERT)
    )
    fun fetchReceiptsOosData() {
        val result = priorityService.receiptsToOosPriorityCoeffs(listOf(RECEIPT_1_KEY, RECEIPT_2_KEY))
        assertEquals(BigDecimal.valueOf(2.5007), result[RECEIPT_1_KEY])
        assertEquals(BigDecimal.valueOf(0.3228), result[RECEIPT_2_KEY])
    }

    @Test
    fun receiptDurations() {
        val rsh1 = ReceiptStatusHistory.builder()
            .receiptKey(RECEIPT_1_KEY)
            .addDate(Instant.parse("2022-04-19T06:01:00Z"))
            .build()
        val rsh2 = ReceiptStatusHistory.builder()
            .receiptKey(RECEIPT_2_KEY)
            .addDate(Instant.parse("2022-04-18T14:01:00Z"))
            .build()
        val rsh3 = ReceiptStatusHistory.builder()
            .receiptKey(RECEIPT_3_KEY)
            .addDate(Instant.parse("2022-04-19T08:40:00Z"))
            .build()
        val receiptSlas = mapOf(
            RECEIPT_1_KEY to 120,
            RECEIPT_2_KEY to 18,
            RECEIPT_3_KEY to 18,
        )
        val actual = priorityService.receiptDurations(listOf(rsh1, rsh2, rsh3), receiptSlas)
        val expected = mapOf(RECEIPT_1_KEY to -118, RECEIPT_2_KEY to 0, RECEIPT_3_KEY to -17)
        assertEquals(expected, actual)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/service/before-different-sla.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/service/sku_oos.xml", type = DatabaseOperation.INSERT)
    )
    fun differentSLA() {
        val prepareCalcData = priorityService.prepareCalcData()
        assertThat(
            prepareCalcData.toTypedArray(),
            arrayContainingInAnyOrder(
                PriorityCalcData(
                    receiptKey = receipt1.receiptKey,
                    receiptTypeCoeff = BigDecimal.ZERO,
                    supplierBonusCoeff = BigDecimal.ZERO,
                    oos = BigDecimal.valueOf(2.5007),
                    duration = -4,
                    sla = 18,
                    timeIntervalCoeff = BigDecimal.ONE,
                    receiptCapacity = BigDecimal.valueOf(19091, 4),
                    rateCoeff = BigDecimal.valueOf(0, 4),
                    qtyExpected = BigDecimal.valueOf(42000000, 5),
                    palletAcceptanceStartedAt = Instant.parse("2022-04-18T18:20:33Z"),
                    oosPosition = BigDecimal.valueOf(10000, 4),
                    fifoPosition = BigDecimal.valueOf(10000, 4)
                ),
                PriorityCalcData(
                    receiptKey = receipt2.receiptKey,
                    receiptTypeCoeff = BigDecimal.ZERO,
                    supplierBonusCoeff = BigDecimal.ZERO,
                    oos = BigDecimal.valueOf(0.3228),
                    duration = -101,
                    sla = 120,
                    timeIntervalCoeff = BigDecimal.ZERO,
                    receiptCapacity = BigDecimal.valueOf(36910, 4),
                    rateCoeff = BigDecimal.valueOf(10000, 4),
                    qtyExpected = BigDecimal.valueOf(81200000, 5),
                    palletAcceptanceStartedAt = Instant.parse("2022-04-18T14:00:00Z"),
                    oosPosition = BigDecimal.valueOf(0, 4),
                    fifoPosition = BigDecimal.valueOf(0, 4)
                ),
            )
        )
    }

    @Test
    fun minMaxNormalization() {
        assertTrue(
            priorityService.normalize(
                value = 1.1.toBigDecimal(),
                min = 1.1.toBigDecimal(),
                max = 1.1.toBigDecimal(),
            ).isZero
        )

        assertNormalization(0.0, -6.0, -6.0, 10.0)
        assertNormalization(0.25, -2.0, -6.0, 10.0)
        assertNormalization(0.375, -0.0, -6.0, 10.0)
        assertNormalization(0.45, 1.2, -6.0, 10.0)
        assertNormalization(1.0, 10.0, -6.0, 10.0)

        assertNormalization(0.0, -100.0, -100.0, -20.0)
        assertNormalization(0.5, -60.0, -100.0, -20.0)
        assertNormalization(1.0, -20.0, -100.0, -20.0)
    }

    private fun assertNormalization(expected: Double, value: Double, min: Double, max: Double) {
        val actual = priorityService.normalize(
            value = value.toBigDecimal(),
            min = min.toBigDecimal(),
            max = max.toBigDecimal()
        )
        assertEquals(
            0,
            expected.toBigDecimal().compareTo(actual),
            "expected: $expected, but actual: $actual"
        )
    }
}
