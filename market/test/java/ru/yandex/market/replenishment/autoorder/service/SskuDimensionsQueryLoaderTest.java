package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SskuDimensionsQueryLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

@ActiveProfiles("unittest")
public class SskuDimensionsQueryLoaderTest extends FunctionalTest {

    @Autowired
    private SskuDimensionsQueryLoader sskuDimensionsQueryLoader;

    @Test
    @DbUnitDataSet(after = "SskuInfoLoaderTest_importSskuDimensions.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mdm/dictionaries/reference_item/1d/latest"
            },
            csv = "SskuInfoLoaderTest_importSskuInfos.yql.csv",
            yqlMock = "SskuDimensionsQueryLoaderTest.yql.mock"
    )
    public void importSskuDimmensionsInfos() {
        sskuDimensionsQueryLoader.load();
    }
}
