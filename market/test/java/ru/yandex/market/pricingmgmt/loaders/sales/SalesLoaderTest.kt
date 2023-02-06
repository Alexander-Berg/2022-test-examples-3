package ru.yandex.market.pricingmgmt.loaders.sales

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.SalesLoader
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDate
import java.time.LocalDateTime

class SalesLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: SalesLoader

    @MockBean
    lateinit var timeService: TimeService

    @Test
    @DbUnitDataSet(
        after = ["SalesLoaderTest_import.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-11",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T04--colon--00--colon--00",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T12--colon--00--colon--00",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/dwh/calculation/fact_new_order_delivery/2021-11",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest"
        ],
        csv = "SalesLoaderTest_import.yql.csv",
        yqlMock = "SalesLoaderTest.yql.mock"
    )
    fun import() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 12, 12))
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2021, 12, 12, 12, 0))
        loader.load()
    }
}
