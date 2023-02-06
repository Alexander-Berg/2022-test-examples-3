package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CategoryManagersLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

@ActiveProfiles("unittest")
public class CategoryManagerLoaderTest extends FunctionalTest {

    @Autowired
    CategoryManagersLoader categoryManagersLoader;

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {"//home/market/production/mstat/dictionaries/mbo/category_managers/latest"},
        csv = "CategoryManagerLoadingTest_importCategoryManagers.yql.csv",
        yqlMock = "CategoryManagerLoadingTest.yql.mock")
    @DbUnitDataSet(
        before = "CategoryManagerLoadingTest_importCategoryManagers.before.csv",
        after = "CategoryManagerLoadingTest_importCategoryManagers.after.csv"
    )
    public void importCategoryManagers() {
        categoryManagersLoader.load();
    }

}
