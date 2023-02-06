package ru.yandex.market.replenishment.autoorder.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CategoryLoader;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;
public class CategoryLoaderTest extends FunctionalTest {

    @Autowired
    CategoryLoader categoryLoaderTest;

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
                            suffix = "\nwhere hid in (90401, 90402, 90403, 90404, 90407)",
                            name = "category.yt.sql"
                    )
            },
            yqlMock = "CategoryLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(after = "CategoryLoaderTest_testLoading.after.csv")
    public void testLoading() {
        categoryLoaderTest.load();
    }

    @Test
    @YqlPrefilledDataTest(
            queries = {
                    @YqlPrefilledDataTest.Query(
                            suffix = "\nwhere hid in (90401, 90402, 90403, 90404, 90407)",
                            name = "category.yt.sql"
                    )
            },
            yqlMock = "CategoryLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(before="CategoryLoaderTest_testExpanding.before.csv",
            after = "CategoryLoaderTest_testExpanding.after.csv")
    public void testExpanding() {
        categoryLoaderTest.load();
    }
}
