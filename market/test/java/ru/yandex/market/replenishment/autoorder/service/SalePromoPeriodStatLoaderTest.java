package ru.yandex.market.replenishment.autoorder.service;


import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SalePromoPeriodStatLoader;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlPrefilledDataTest;
public class SalePromoPeriodStatLoaderTest extends FunctionalTest {

    @Autowired
    private SalePromoPeriodStatLoader loader;

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
                            suffix = " WHERE (msku = 608388320 OR msku = 101364151768);",
                            name = "sale_promo_period_stat.yt.sql"
                    )
            },
            yqlMock = "SalePromoPeriodStatLoaderTest.yql.mock"
    )
    @DbUnitDataSet(after = "SalePromoPeriodStatLoaderTest.after.csv")
    public void testLoading() {
        setTestTime(LocalDateTime.of(2021, 11, 30, 0, 0));
        loader.load();
    }
}
