package ru.yandex.market.forecastint.service.yt.loader

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.forecastint.AbstractFunctionalTest
import ru.yandex.market.forecastint.model.dto.ForecastFilter
import ru.yandex.market.forecastint.service.yt.loader.service.YtSalesService
import ru.yandex.market.yql_test.annotation.YqlTest
import ru.yandex.mj.generated.server.model.TimeIntervalType
import java.time.LocalDate

class YtSalesServiceTest : AbstractFunctionalTest() {
    @Autowired
    private val ytSalesService: YtSalesService? = null

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest",
            "//home/market/production/mbo/export/recent/models/sku"],
        csv = "SalesByDayLoaderTest.yql.csv",
        yqlMock = "YtSalesServiceTest_groupByAllForDays.yql.mock.json"
    )
    @DbUnitDataSet(
        before = ["YtSalesServiceTest.before.csv"]
    )
    fun test_GroupByAllForDays() {
        val result = ytSalesService!!.findSalesGroupedByDate(
            ForecastFilter(
                from = LocalDate.of(2021, 12, 14),
                to = LocalDate.of(2021, 12, 25),
                intervalType = TimeIntervalType.DAY,
                supplierIds = listOf(777L),
                warehouseIds = listOf(171L),
                mskus = listOf(107981L),
                categoryIds = listOf(1L),
                catteamIds = listOf(1L)
            )
        )
        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(LocalDate.of(2021, 12, 14), result[0].date)
        Assertions.assertEquals(1L, result[0].sales)
        Assertions.assertEquals(650.0, result[0].price)
        Assertions.assertEquals(LocalDate.of(2021, 12, 25), result[1].date)
        Assertions.assertEquals(3L, result[1].sales)
        Assertions.assertEquals(653.0, result[1].price)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest",
            "//home/market/production/mbo/export/recent/models/sku"],
        csv = "SalesByDayLoaderTest.yql.csv",
        yqlMock = "YtSalesServiceTest_groupWithoutMskuForDays.yql.mock.json"
    )
    fun test_GroupWithoutMskuForDays() {
        val result = ytSalesService!!.findSalesGroupedByDate(
            ForecastFilter(
                from = LocalDate.of(2021, 12, 14),
                to = LocalDate.of(2021, 12, 25),
                intervalType = TimeIntervalType.DAY,
                supplierIds = listOf(777L),
                warehouseIds = listOf(171L),
            )
        )
        Assertions.assertEquals(3, result.size)
        Assertions.assertEquals(LocalDate.of(2021, 12, 14), result[0].date)
        Assertions.assertEquals(1L, result[0].sales)
        Assertions.assertEquals(650.0, result[0].price)
        Assertions.assertEquals(LocalDate.of(2021, 12, 16), result[1].date)
        Assertions.assertEquals(16L, result[1].sales)
        Assertions.assertEquals(2634.0, result[1].price)
        Assertions.assertEquals(LocalDate.of(2021, 12, 25), result[2].date)
        Assertions.assertEquals(3L, result[2].sales)
        Assertions.assertEquals(653.0, result[2].price)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest",
            "//home/market/production/mbo/export/recent/models/sku"],
        csv = "SalesByDayLoaderTest.yql.csv",
        yqlMock = "YtSalesServiceTest_groupByCountryForDays.yql.mock.json"
    )
    fun test_groupByCountryForDays() {
        val result = ytSalesService!!.findSalesGroupedByDate(
            ForecastFilter(
                from = LocalDate.of(2021, 12, 15),
                to = LocalDate.of(2021, 12, 27),
                intervalType = TimeIntervalType.DAY
            )
        )
        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(LocalDate.of(2021, 12, 16), result[0].date)
        Assertions.assertEquals(16L, result[0].sales)
        Assertions.assertEquals(2634.0, result[0].price)
        Assertions.assertEquals(LocalDate.of(2021, 12, 25), result[1].date)
        Assertions.assertEquals(3L, result[1].sales)
        Assertions.assertEquals(653.0, result[1].price)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest",
            "//home/market/production/mbo/export/recent/models/sku"],
        csv = "SalesByDayLoaderTest.yql.csv",
        yqlMock = "YtSalesServiceTest_groupBySupplierForWeeks.yql.mock.json"
    )
    fun test_GroupBySupplierForWeeks() {
        val result = ytSalesService!!.findSalesGroupedByDate(
            ForecastFilter(
                from = LocalDate.of(2021, 12, 13),
                to = LocalDate.of(2021, 12, 27),
                intervalType = TimeIntervalType.WEEK,
                supplierIds = listOf(777L),
            )
        )
        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(LocalDate.of(2021, 12, 13), result[0].date)
        Assertions.assertEquals(17L, result[0].sales)
        Assertions.assertEquals(3284.0, result[0].price)
        Assertions.assertEquals(LocalDate.of(2021, 12, 20), result[1].date)
        Assertions.assertEquals(3L, result[1].sales)
        Assertions.assertEquals(653.0, result[1].price)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest",
            "//home/market/production/mbo/export/recent/models/sku"],
        csv = "SalesByDayLoaderTest.yql.csv",
        yqlMock = "empty.yql.mock.json"
    )
    fun test_tooBigRequest() {
        val filter = ForecastFilter(
            from = LocalDate.of(2015, 12, 15),
            to = LocalDate.of(2020, 12, 27),
            intervalType = TimeIntervalType.DAY
        )
        Assertions.assertThrows(IllegalArgumentException::class.java) { ytSalesService!!.findSalesGroupedByDate(filter) }
    }
}
