package ru.yandex.market.pricingmgmt.loaders.offer_settings

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.OfferSettingsLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class OfferSettingsLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: OfferSettingsLoader

    @Test
    @DbUnitDataSet(after = ["OfferSettingsLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/testing/monetize/dynamic_pricing/category_interface/pricing_mgmt/offer_settings/latest"
        ],
        csv = "OfferSettingsLoaderTest_import.yql.csv",
        yqlMock = "OfferSettingsLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
