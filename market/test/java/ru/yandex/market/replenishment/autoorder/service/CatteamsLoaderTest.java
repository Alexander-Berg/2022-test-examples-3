package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CatteamRepository;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CatteamsLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class CatteamsLoaderTest extends FunctionalTest {

    @Autowired
    private CatteamsLoader catteamsLoader;

    @Autowired
    private CatteamRepository catteamRepository;

    @Test
    @DbUnitDataSet(
            after = "CatteamLoader_importCatteams.after.csv",
            before = "CatteamLoader_importCatteams.before.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/mbo/catteam/latest"
            },
            csv = "CatteamLoaderTest_importCatteams.yql.csv",
            yqlMock = "CatteamLoaderTest.yql.mock"
    )
    public void importCampaign() {
        catteamRepository.save("catteam1");
        catteamsLoader.load();
    }

}
