package ru.yandex.market.pricingmgmt.loaders.assortment

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.AssortmentLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class AssortmentLoaderTest : ControllerTest() {
    @Autowired
    lateinit var assortmentLoader: AssortmentLoader

    @Test
    @DbUnitDataSet(
        before = ["AssortmentLoaderTest_import.before.csv"],
        after = ["AssortmentLoaderTest_import.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_golden_matrix",
            "//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest",
            "//home/market/production/mbo/export/recent/models/sku",
            "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest"
        ],
        csv = "AssortmentLoaderTest_import.yql.csv",
        yqlMock = "AssortmentLoaderTest.yql.mock"
    )
    fun import() {
        assortmentLoader.load()
    }
}
