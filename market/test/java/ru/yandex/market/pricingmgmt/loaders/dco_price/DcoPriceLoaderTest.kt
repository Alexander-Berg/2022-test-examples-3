package ru.yandex.market.pricingmgmt.loaders.dco_price

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.DcoPriceLoader
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.market.yql_test.annotation.YqlTest

open class DcoPriceLoaderTest : ControllerTest() {
    companion object {
        private val MOCK_DATE_TIME = DateTimeTestingUtil.createOffsetDateTime(2022,2, 23, 12,0,0)
    }

    @Autowired
    lateinit var loader: DcoPriceLoader

    @MockBean
    lateinit var timeService: TimeService

    @BeforeEach
    open fun mockDateTime() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(MOCK_DATE_TIME)
    }

    @Test
    @DbUnitDataSet(
        before = ["DcoPriceLoaderTest.before.csv"],
        after = ["DcoPriceLoaderTest.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/testing/monetize/dynamic_pricing/category_interface/prices_snapshot"
        ],
        csv = "DcoPriceLoaderTest.yql.csv",
        yqlMock = "DcoPriceLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["DcoPriceLoaderTest.before.not_run.csv"],
        after = ["DcoPriceLoaderTest.before.not_run.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/testing/monetize/dynamic_pricing/category_interface/prices_snapshot"
        ],
        csv = "DcoPriceLoaderTest.yql.csv",
        yqlMock = "DcoPriceLoaderTest.not_run.yql.mock"
    )
    fun import_notAllowedToRun() {
        loader.load()
    }

    @Test
    @DbUnitDataSet(
        before = ["DcoPriceLoaderTest.duplicates.csv"],
        after = ["DcoPriceLoaderTest.duplicates.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/testing/monetize/dynamic_pricing/category_interface/prices_snapshot"
        ],
        csv = "DcoPriceLoaderTest.duplicates.yql.csv",
        yqlMock = "DcoPriceLoaderTest.duplicates.yql.mock"
    )
    fun import_foundDuplicates() {
        val ex = assertThrows(RuntimeException::class.java) {
            loader.load()
        }

        val expected = StringBuilder()
        expected.append("Abort loading DCO prices because found SSKU duplicates:")
        expected.append(" {ssku=130.abc, priceTypeId=60, warehouseId=null, duplicatesCount=2},")
        expected.append(" {ssku=110.abc, priceTypeId=50, warehouseId=null, duplicatesCount=2},")
        expected.append(" {ssku=120.abc, priceTypeId=50, warehouseId=301, duplicatesCount=2}")

        assertEquals(expected.toString(), ex.message)
    }
}
