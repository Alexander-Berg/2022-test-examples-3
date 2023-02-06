package ru.yandex.market.pricingmgmt.loaders.partner

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.PartnersLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class PartnerLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: PartnersLoader

    @Test
    @DbUnitDataSet(
        before = ["PartnerLoaderTest_import.before.csv"],
        after = ["PartnerLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = ["//home/market/testing/mbi/dictionaries/partner_biz_snapshot/latest"],
        csv = "PartnerLoaderTest_import.yql.csv",
        yqlMock = "PartnerLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
