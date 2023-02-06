package ru.yandex.market.replenishment.autoorder.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.ManufacturerLoader;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;
public class ManufacturerLoaderTest extends FunctionalTest {

    @Autowired
    ManufacturerLoader manufacturerLoader;

    @Autowired
    TableNamesTestQueryService tableNamesTestQueryService;

    @Before
    public void setUp() {
        tableNamesTestQueryService.setNotUseLocalTables(true);
    }
    @After
    public void cleanUp() {
        tableNamesTestQueryService.setNotUseLocalTables(false);
    }

    @Test
    @YqlPrefilledDataTest(
            queries = {
                    @YqlPrefilledDataTest.Query(
                            suffix = " and manufacturer_name in ('Шарлиз', 'Фабрика игрушек')",
                            name = "manufacturer.yt.sql"
                    )
            },
            yqlMock = "ManufacturerLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(after = "ManufacturerLoaderTest_testLoading.after.csv")
    public void testLoading() {
        manufacturerLoader.load();
    }
}
