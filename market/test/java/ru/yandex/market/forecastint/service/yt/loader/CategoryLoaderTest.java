package ru.yandex.market.forecastint.service.yt.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class CategoryLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private CategoryLoader categoryLoaderTest;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbo/export/recent/tovar-tree"
            },
            csv = "CategoryLoaderTest_testLoading.yql.csv",
            yqlMock = "CategoryLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(after = "CategoryLoaderTest_testLoading.after.csv")
    public void testLoading() {
        categoryLoaderTest.load();
    }
}
