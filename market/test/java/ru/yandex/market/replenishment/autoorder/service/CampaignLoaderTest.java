package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CampaignRepository;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CampaignLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class CampaignLoaderTest extends FunctionalTest {
    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    CampaignLoader campaignLoader;

    @Test
    @DbUnitDataSet(after = "CampaignLoaderTest_importCampaign.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/mbi_suppliers/latest"
            },
            csv = "CampaignLoaderTest_importCampaign.yql.csv",
            yqlMock = "CampaignLoaderTest.yql.mock"
    )

    public void importCampaign() {
        campaignLoader.load();
    }
}
