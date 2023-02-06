package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.MonoXdocUnionXdocLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class MonoXdocUnionXdocLoaderTest extends FunctionalTest {

    @Autowired
    private MonoXdocUnionXdocLoader loader;

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {"//home/market/production/replenishment/manual/union_restrictions/xdoc"},
        csv = "MonoXdocUnionXdocLoaderTest_import.yql.csv",
        yqlMock = "MonoXdocUnionXdocLoaderTest.yql.mock")
    @DbUnitDataSet(
        after = "MonoXdocUnionXdocLoaderTest_import.after.csv"
    )
    public void loadTest() {
        loader.load();
    }

}
