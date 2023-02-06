package ru.yandex.market.pricingmgmt.loaders.competitorPrice

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.CompetitorPriceLoader
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.yql_test.annotation.YqlTest
import ru.yandex.market.yt.util.table.YtTableService
import java.time.LocalDateTime

class CompetitorPriceLoaderTest : ControllerTest() {

    @Autowired
    lateinit var loader: CompetitorPriceLoader

    @MockBean
    lateinit var timeService: TimeService

    @MockBean
    @Qualifier("ytHahnTableService")
    lateinit var ytHahnTableService: YtTableService

    @Test
    @DbUnitDataSet(after = ["CompetitorPriceLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table/2022-02-01T16--colon--00--colon--00"
        ],
        csv = "CompetitorPriceLoaderTest_import.yql.csv",
        yqlMock = "CompetitorPriceLoaderTest.yql.mock"
    )
    fun import() {
        Mockito.`when`(ytHahnTableService.isTableExists(any())).thenReturn(true)
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2022, 2, 1, 16, 0))
        loader.load()
    }

    @Test
    @DbUnitDataSet(after = ["CompetitorPriceLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table/2022-02-01T16--colon--00--colon--00"
        ],
        csv = "CompetitorPriceLoaderTest_import.yql.csv",
        yqlMock = "CompetitorPriceLoaderTest.yql.mock"
    )
    fun import_whenTableNotExistUsePrevious() {
        Mockito.`when`(ytHahnTableService.isTableExists(any())).thenReturn(false)
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2022, 2, 1, 20, 0))
        loader.load()
    }
}
