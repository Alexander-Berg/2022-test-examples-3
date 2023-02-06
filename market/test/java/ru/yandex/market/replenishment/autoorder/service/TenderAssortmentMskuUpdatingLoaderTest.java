package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.sku_transition.TenderAssortmentMskuUpdatingLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class TenderAssortmentMskuUpdatingLoaderTest extends FunctionalTest {

    @Autowired
    private TenderAssortmentMskuUpdatingLoader loader;

    @Test
    @DbUnitDataSet(
            before = "TenderAssortmentMskuUpdatingLoaderTest_import.before.csv",
            after = "TenderAssortmentMskuUpdatingLoaderTest_import.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/2021-05-18",
            },
            csv = "TenderAssortmentMskuUpdatingLoaderTest_import.yql.csv",
            yqlMock = "TenderAssortmentMskuUpdatingLoaderTest.yql.mock"
    )
    public void importTest() {
        setTestTime(LocalDateTime.of(2021, 5, 20, 0, 0));
        loader.load();
    }

}
