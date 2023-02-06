package ru.yandex.market.replenishment.autoorder.repository;

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.ABC
import ru.yandex.market.replenishment.autoorder.model.SskuStatus
import ru.yandex.market.replenishment.autoorder.model.dto.TenderResultForExcelDTO
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderResult
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderResultStatistic
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderResultRepository
import java.util.stream.Collectors

const val EPS = 1e-6

class TenderResultRepositoryTest : FunctionalTest() {
    @Autowired
    private lateinit var tenderResultRepository: TenderResultRepository

    @Test
    @DbUnitDataSet(after = ["TenderResultRepositoryTest_insert.after.csv"])
    fun insertTest() {
        val gson = GsonBuilder().create()
        val statistic = TenderResultStatistic(1L, ABC.A, true, 1.0, 1.0)
        tenderResultRepository.insert(
            TenderResult(
                1,
                2,
                3,
                "4",
                2,
                1.0,
                1,
                "text",
                gson.toJson(statistic),
                warehouseId = 172
            )
        )
    }

    @Test
    @DbUnitDataSet(after = ["TenderResultRepositoryTest_insertNullParams.after.csv"])
    fun insertNullParamsTest() {
        tenderResultRepository.insert(
            TenderResult(1, 2, 3, "4", 2, 1.0, 1,"text"))
    }

    @Test
    @DbUnitDataSet(before = ["TenderResultRepositoryTest_findByDemandId.before.csv"])
    fun findByDemandIdTest() {
        val tenderResults = tenderResultRepository.findByDemandId(1L)
        assertEquals(1, tenderResults.size.toLong())
        val actual = tenderResults[0]
        assertEquals(2L, actual.msku)
        assertEquals(3L, actual.supplierId)
        assertEquals(2L, actual.items)
        assertEquals("4", actual.ssku)
        assertEquals(1.0, actual.price!!, EPS)
        assertEquals("text1", actual.comment)
        assertEquals("sub_sub_category_1", actual.categoryName)
        assertEquals(1L, actual.demandId)
        assertEquals("Шедевры Ван Гога", actual.assortmentTitle)
        assertEquals(3L, actual.shipmentQuantum)
        assertEquals(5, actual.leadTime)
        assertEquals(actual.items!! * actual.price!!, actual.sum!!, 0.01)
        assertEquals(SskuStatus.INACTIVE, actual.sskuStatus)
        assertEquals(145L, actual.warehouseId)
        assertNotNull(actual.params)
        val (_, abc, isCoreFix, salePrice, competitorPrice) = Gson().fromJson(
            actual.params,
            TenderResultStatistic::class.java
        )
        assertEquals(ABC.A, abc)
        assertTrue(isCoreFix!!)
        assertEquals(1.0, salePrice!!, EPS)
        assertEquals(1.0, competitorPrice!!, EPS)
        assertEquals(49L, actual.purchQty)
    }

    @DbUnitDataSet(before = ["TenderResultRepositoryTest_getTenderResultStatistic.before.csv"])
    @Test
    fun tenderResultStatisticTest() {
        val statistics = tenderResultRepository.getTenderResultStatistic(10L, setOf(1L, 2L, 3L))
        val statistic1 = statistics[1L]
        assertNotNull(statistic1)
        assertEquals(1, statistic1!!.msku as Long)
        assertTrue(statistic1.isCoreFix!!)
        assertEquals(42.0, statistic1.competitorPrice!!, EPS)
        assertEquals(1.0, statistic1.salePrice!!, EPS)
        assertEquals(6.0, statistic1.sf56!!, EPS)

        val statistic2 = statistics[2L]
        assertNotNull(statistic2)
        assertEquals(2, statistic2!!.msku as Long)
        assertFalse(statistic2.isCoreFix!!)
        assertEquals(43.0, statistic2.competitorPrice!!, EPS)
        assertEquals(2.0, statistic2.salePrice!!, EPS)
        assertEquals(null, statistic2.sf56)

        val statistic3 = statistics[3L]
        assertNotNull(statistic3)
        assertEquals(3, statistic3!!.msku as Long)
        assertTrue(statistic3.isCoreFix!!)
        assertEquals(15.0, statistic3.competitorPrice!!, EPS)
        assertNull(statistic3.salePrice)
        assertEquals(null, statistic3.sf56)
    }

    @Test
    @DbUnitDataSet(before = ["TenderResultRepositoryTest_findByDemandIdForExcel.before.csv"])
    fun findByDemandIdForExcelTest() {
        val tenderResults = tenderResultRepository.findByDemandIdForExcel(7)
        assertEquals(5, tenderResults.size.toLong())
        val testSsku = "gg8888"
        val testList = tenderResults.stream()
            .filter { (_, ssku): TenderResultForExcelDTO -> testSsku == ssku }
            .collect(Collectors.toList())
        assertEquals(1, testList.size.toLong())
        var testDTO = testList[0]
        assertEquals(883366L, testDTO.msku!!.toLong())
        assertEquals(300L, testDTO.purchQty!!.toLong())
        assertEquals("Marvel", testDTO.supplierName)
        assertEquals(48.0, testDTO.price!!, EPS)
        assertEquals(200L, testDTO.items!!.toLong())
        assertEquals("Last price: 50", testDTO.comment)
        assertEquals("BONAS", testDTO.vendorName)
        assertEquals("code-Ant", testDTO.vendorCode)
        assertEquals("Носки", testDTO.category1)
        assertEquals("Нижнее белье", testDTO.category2)
        assertEquals("Одежда", testDTO.category3)
        assertEquals("Одежда", testDTO.category3)
        assertEquals("Ant", testDTO.assortmentTitle)
        assertEquals(5L, testDTO.shipmentQuantum)
        assertEquals(5, testDTO.leadTime)
        assertEquals("Маршрут", testDTO.warehouseName)
        assertEquals(testDTO.items!! * testDTO.price!!, testDTO.sum!!.toDouble(), 0.01)
        assertEquals(300L, testDTO.purchQty)
        assertEquals(SskuStatus.INACTIVE, testDTO.sskuStatus)
        assertTrue(testDTO.isCoreFix!!)
        assertEquals(1, testDTO.salesWeek1 as Long)
        assertEquals(1, testDTO.salesWeek2 as Long)
        assertEquals(1, testDTO.salesWeek3 as Long)
        assertEquals(1, testDTO.salesWeek4 as Long)
        assertEquals(0, testDTO.salesMonth2 as Long)
        assertEquals(21, testDTO.salesMonth3 as Long)
        assertEquals(22, testDTO.stocks1month as Long)
        assertEquals(6, testDTO.stocks as Long)
        assertEquals(48.0, testDTO.salePrice!!, EPS)
        assertEquals(42.0, testDTO.competitorPrice!!, EPS)
        val testSsku2 = "www85858"
        val testList2 = tenderResults.stream()
            .filter { (_, ssku): TenderResultForExcelDTO -> testSsku2 == ssku }
            .collect(Collectors.toList())
        assertEquals(1, testList.size.toLong())
        testDTO = testList2[0]
        assertEquals(994455L, testDTO.msku!!.toLong())
        assertEquals(400L, testDTO.purchQty!!.toLong())
        assertEquals("Lego", testDTO.supplierName)
        assertEquals(42.0, testDTO.price!!, EPS)
        assertEquals(300L, testDTO.items!!.toLong())
        assertEquals("Last price: 70", testDTO.comment)
        assertEquals("BONAS", testDTO.vendorName)
        assertEquals("code-Dragonfly", testDTO.vendorCode)
        assertEquals("Пижамы", testDTO.category1)
        assertEquals("Нижнее белье", testDTO.category2)
        assertEquals("Одежда", testDTO.category3)
        assertEquals("Одежда", testDTO.category3)
        assertEquals("Dragonfly", testDTO.assortmentTitle)
        assertEquals(7L, testDTO.shipmentQuantum)
        assertEquals(3, testDTO.leadTime)
        assertEquals("Софьино", testDTO.warehouseName)
        assertEquals(testDTO.items!! * testDTO.price!!, testDTO.sum!!.toDouble(), 0.01)
        assertNull(testDTO.isCoreFix)
        assertEquals(0, testDTO.salesWeek1 as Long)
        assertEquals(0, testDTO.salesWeek2 as Long)
        assertEquals(0, testDTO.salesWeek3 as Long)
        assertEquals(0, testDTO.salesWeek4 as Long)
        assertEquals(0, testDTO.salesMonth2 as Long)
        assertEquals(0, testDTO.salesMonth3 as Long)
        assertEquals(0, testDTO.stocks1month as Long)
        assertEquals(0, testDTO.stocks as Long)
        assertNull(testDTO.salePrice)
        assertNull(testDTO.competitorPrice)
        assertEquals(400L, testDTO.purchQty)
        assertEquals(SskuStatus.ACTIVE, testDTO.sskuStatus)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderResultRepositoryTest_testCompleteDemandIfAllSent.before.csv"],
        after = ["TenderResultRepositoryTest_testCompleteDemandIfAllSent_Complete.after.csv"]
    )
    fun testCompleteDemandIfAllSent_Complete() {
        tenderResultRepository.completeDemandIfAllSent(2L)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderResultRepositoryTest_testCompleteDemandIfAllSent.before.csv"],
        after = ["TenderResultRepositoryTest_testCompleteDemandIfAllSent_NotComplete.after.csv"]
    )
    fun testCompleteDemandIfAllSent_NotComplete() {
        tenderResultRepository.completeDemandIfAllSent(1L)
    }

    @Test
    @DbUnitDataSet(before = ["TenderResultRepositoryTest_testGetNotProcessedSupplierIds.before.csv"])
    fun testGetNotProcessedSupplierIds() {
        val actualIds = tenderResultRepository.getNotProcessedSupplierIds(1L, listOf(1L, 2L, 3L))
        assertEquals(1, actualIds.size.toLong())
        assertTrue(actualIds.contains(3L))
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderResultRepositoryTest_updateBySsku.before.csv"],
        after = ["TenderResultRepositoryTest_updateBySsku.after.csv"]
    )
    fun updateBySskuTest() {
        tenderResultRepository.updateBySsku(1, "4", 42L)
        tenderResultRepository.updateBySsku(1, "5", 0L)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderResultRepositoryTest_updateWarehouseId.before.csv"],
        after = ["TenderResultRepositoryTest_updateWarehouseId.after.csv"]
    )
    fun updateWarehouseId() {
        tenderResultRepository.updateWarehouseId(1, "4", 172)
        tenderResultRepository.updateWarehouseId(1, "5", 304)
        tenderResultRepository.updateWarehouseId(1, "6", null)
    }
}
