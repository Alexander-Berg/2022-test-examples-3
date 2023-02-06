package ru.yandex.market.pricingmgmt.repository.postgres.prices_selection

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.loaders.HackSskuPromoAggregateLoader
import ru.yandex.market.pricingmgmt.loaders.PriceSelectionLoader
import ru.yandex.market.pricingmgmt.repository.postgres.PriceSelectionRepository
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import java.time.OffsetDateTime
import java.util.Collections

class PriceSelectionTest(
    @Autowired private val hackSskuPromoAggregateLoader: HackSskuPromoAggregateLoader,
    @Autowired private val priceSelectionRepository: PriceSelectionRepository,
    @Autowired private val priceSelectionLoader: PriceSelectionLoader,
    @Autowired private val timeService: TimeService,
) : AbstractFunctionalTest() {

    companion object {
        private fun getOffsetDateTime(h: Int, m: Int): OffsetDateTime = DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, h, m, 0)
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testTruncate.before.csv"],
        after = ["PriceSelectionRepositoryTest.testTruncate.after.csv"]
    )
    fun testTruncate() {
        priceSelectionRepository.truncateExpandedPrices()
        priceSelectionRepository.truncateSelectedPrices()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testExpand.before.csv"],
        after = ["PriceSelectionRepositoryTest.testExpandAllActiveJournals.after.csv"]
    )
    fun testExpandAllActiveJournals() {
        priceSelectionRepository.expandAllActiveJournals(getOffsetDateTime(9, 0))
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testExpand.before.csv"],
        after = ["PriceSelectionRepositoryTest.testExpandJournals.after.csv"]
    )
    fun testExpandJournals() {
        priceSelectionRepository.expandJournals(
            Collections.singletonList(1L),
            Collections.singletonList(1L)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testRepisotorySelectSimple.before.csv"],
        after = ["PriceSelectionRepositoryTest.testRepisotorySelectSimple.after.csv"]
    )
    fun testRepositorySelectSimple() {
        priceSelectionRepository.selectPrices()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testRepisotorySelectJournalAndDco.before.csv"],
        after = ["PriceSelectionRepositoryTest.testRepisotorySelectJournalAndDco.after.csv"]
    )
    fun testRepisotorySelectJournalAndDco() {
        priceSelectionRepository.expandAllDcoPrices()
        priceSelectionRepository.expandAllActiveJournals(getOffsetDateTime(9, 0))
        priceSelectionRepository.selectPrices()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectSimple.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectSimple.after.csv"]
    )
    fun testSelectSimple() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectDcoWithWh.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectDcoWithWh.after.csv"]
    )
    fun testSelectDcoWithWh() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectFlap.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectFlap.after.csv"]
    )
    fun testSelectFlap() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2022, 5, 30, 15, 57, 37)
        )

        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectPromoFreeze.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectPromoFreeze.after.csv"]
    )
    fun testSelectPromoFreeze() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2022, 5, 30, 15, 57, 37)
        )

        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectHistoryFull.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectHistoryFull.after.csv"]
    )
    fun testSelectHistoryFull() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectHistoryJournals.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectHistoryJournals.after.csv"]
    )
    fun testSelectHistoryJournals() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectWh.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectWh.after.csv"]
    )
    fun testSelectWithWarehouses() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectJournalAndDco.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectJournalAndDco.after.csv"]
    )
    fun testSelectJournalAndDco() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectJournalAndDcoAfterDcoUpdate.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectJournalAndDcoAfterDcoUpdate.after.csv"]
    )
    fun testSelectJournalAndDcoAfterDcoUpdate() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 1))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectCancel.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectCancel.after.csv"]
    )
    fun testSelectWithCancelPrices() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlags.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlags.after.csv"]
    )
    fun testSelectWithBusinessFlags() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareRestric.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareRestric.after.csv"]
    )
    fun testSelectWithBusinessFlagsPromoAwareRestric() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        hackSskuPromoAggregateLoader.run()
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareAllow.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareAllow.after.csv"]
    )
    fun testSelectWithBusinessFlagsPromoAwareAllow() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        hackSskuPromoAggregateLoader.run()
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareRestricDisableEnvVar.before.csv"],
        after = ["PriceSelectionRepositoryTest.testSelectWithBusinessFlagsPromoAwareRestricDisableEnvVar.after.csv"]
    )
    fun testSelectWithBusinessFlagsPromoAwareRestricDisableEnvVar() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        hackSskuPromoAggregateLoader.run()
        priceSelectionLoader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["PriceSelectionRepositoryTest.testIncrementalPriceLog.before.csv"],
        after = ["PriceSelectionRepositoryTest.testIncrementalPriceLog.after.csv"]
    )
    fun testIncrementalPriceLog() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(getOffsetDateTime(8, 0))
        priceSelectionLoader.load()
    }
}
