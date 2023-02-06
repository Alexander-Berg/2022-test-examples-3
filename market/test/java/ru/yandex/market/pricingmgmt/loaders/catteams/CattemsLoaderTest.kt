package ru.yandex.market.pricingmgmt.loaders.catteams

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.CatteamsLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class CattemsLoaderTest : ControllerTest() {

    @Autowired
    lateinit var loader: CatteamsLoader

    @Test
    @DbUnitDataSet(
        before = ["CatteamsLoaderTest.before.csv"],
        after = ["CatteamsLoaderTest.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/mbo/catteam/latest"
        ],
        csv = "CatteamsLoaderTest.yql.csv",
        yqlMock = "CatteamsLoaderTest.yql.mock"
    )
    fun importCatteams() {
        loader.load()
    }
}
