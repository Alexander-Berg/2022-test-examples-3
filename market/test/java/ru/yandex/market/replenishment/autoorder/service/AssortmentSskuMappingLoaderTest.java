package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.AssortmentSskuMappingLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

@ActiveProfiles("unittest")
public class AssortmentSskuMappingLoaderTest extends FunctionalTest {

    @Autowired
    AssortmentSskuMappingLoader assortmentSskuMappingLoader;

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {"//home/market/prestable/mstat/dictionaries/mbo/warehouse_service/2022-01-28"},
        csv = "AssortmentSskuMappingLoaderTest_importAssortmentSskuMapping.yql.csv",
        yqlMock = "AssortmentSskuMappingLoaderTest.yql.mock")
    @DbUnitDataSet(
        after = "AssortmentSskuMappingLoaderTest_importAssortmentSskuMapping.after.csv"
    )
    public void importAssortmentSskuMapping() {
        setTestTime(LocalDateTime.of(2022, 1, 29, 0, 0));
        assortmentSskuMappingLoader.load();
    }

}
