package ru.yandex.market.replenishment.autoorder.service.tender

import org.apache.ibatis.session.SqlSession
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderSupplierResponse
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderSupplierResponseRepository
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.service.tender.decision_tree.PalettesCostCalculatorFactory

const val EPS: Double = 1E-3

@WithMockLogin
class PalettesCostCalculatorTest : FunctionalTest() {

    @Autowired
    private lateinit var palettesCostCalculatorFactory: PalettesCostCalculatorFactory

    @Autowired
    private lateinit var batchSqlSession: SqlSession

    @Test
    @DbUnitDataSet(
        before = ["PalettesCostCalculatorTest.getFromDb.before.csv"]
    )
    fun getFromDb() {
        val demandId = 1L
        val tenderResponses = getResponseRepo().getForDecisionTree(demandId, listOf(147L))
        val calculator = palettesCostCalculatorFactory.getByPalettesCostCalculator(demandId, tenderResponses)

        assertEquals(16.0, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(1L, 1L)))
        assertEquals(8.0, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(1L, 2L)))
        assertEquals(20.0, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(2L, 1L)))
        assertEquals(10.0, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(2L, 2L)))
    }

    @Test
    @DbUnitDataSet(
        before = ["PalettesCostCalculatorTest.calculate.before.csv"],
        after = ["PalettesCostCalculatorTest.calculate.after.csv"]
    )
    fun calculate() {
        val demandId = 1L
        val tenderResponses = getResponseRepo().getForDecisionTree(demandId, listOf(147L))
        val calculator = palettesCostCalculatorFactory.getByPalettesCostCalculator(demandId, tenderResponses)

        assertEquals(2.2857, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(1L, 1L)), EPS)
        assertEquals(9.4117, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(1L, 2L)), EPS)
        assertEquals(3.5714, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(2L, 1L)), EPS)
        assertEquals(10.5263, calculator.getMskuPriceOfPalette(tenderResponses.getResponse(2L, 2L)), EPS)
    }

    @Test
    @DbUnitDataSet(
        before = ["PalettesCostCalculatorTest.calculate_paletteCountLessThanBox.before.csv"],
        after = ["PalettesCostCalculatorTest.calculate_paletteCountLessThanBox.after.csv"]
    )
    fun calculate_paletteCountLessThanBox() {
        val demandId = 1L
        val tenderResponses = getResponseRepo().getForDecisionTree(demandId, listOf(147L))
        palettesCostCalculatorFactory.getByPalettesCostCalculator(demandId, tenderResponses)
    }

    private fun getResponseRepo() = batchSqlSession.getMapper(TenderSupplierResponseRepository::class.java)
}

fun Collection<TenderSupplierResponse>.getResponse(supplierId: Long, msku: Long): TenderSupplierResponse =
    this.first { response -> response.supplierId == supplierId && response.msku == msku }
