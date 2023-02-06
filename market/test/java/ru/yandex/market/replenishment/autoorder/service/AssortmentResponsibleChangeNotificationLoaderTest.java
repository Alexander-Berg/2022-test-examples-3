package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.sku_transition.AssortmentResponsibleChangeNotificationLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class AssortmentResponsibleChangeNotificationLoaderTest extends FunctionalTest {

    @Autowired
    private AssortmentResponsibleChangeNotificationLoader loader;

    @Test
    @DbUnitDataSet(
            after = "AssortmentResponsibleChangeNotificationLoaderTest_import.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/mstat/dictionaries/autoorder/assortment_responsibles/latest",
                    "//home/market/production/mstat/dictionaries/autoorder/assortment_responsibles/2021-05-24"
            },
            csv = "AssortmentResponsibleChangeNotificationLoaderTest_import.yql.csv",
            yqlMock = "AssortmentResponsibleChangeNotificationLoaderTest.yql.mock"
    )
    public void importTest() {
        setTestTime(LocalDateTime.of(2021, 5, 25, 0, 0));
        loader.load();
    }

}
