package ru.yandex.market.rg.config;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(dataSource = "clickHouseDataSource",
        nonTruncatedTables = {
                "default.tmp_brands",
                "default.vendors",
                "default.tmp_categories",
                "default.categories"
        })
public class ClickhouseFunctionalTest extends FunctionalTest {

}
