package ru.yandex.market.pricingmgmt.loaders.sskuInfo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.SskuInfoLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class SskuInfoLoaderTest : ControllerTest() {

    @Autowired
    lateinit var sskuInfoLoader: SskuInfoLoader

    @Test
    @DbUnitDataSet(
        before = ["SskuInfoLoaderTest_importSskuInfos.before.csv"],
        after = ["SskuInfoLoaderTest_importSskuInfos.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mbo/stat/mboc_offers/latest",
            "//home/market/production/mbo/mboc/offer-mapping",
            "//home/market/production/mstat/dictionaries/mdm/master_data/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest"
        ],
        csv = "SskuInfoLoaderTest_importSskuInfos.yql.csv",
        yqlMock = "SskuInfoLoaderTest.yql.mock"
    )
    fun importSskuInfos() {
        sskuInfoLoader.load()
    }
}
