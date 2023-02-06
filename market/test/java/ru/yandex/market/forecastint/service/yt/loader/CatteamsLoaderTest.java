package ru.yandex.market.forecastint.service.yt.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.repository.postgres.CatteamRepository;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class CatteamsLoaderTest extends AbstractFunctionalTest {

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
            yqlMock = "CatteamLoaderTest.yql.mock.json"
    )
    public void importCatteams() {
        catteamRepository.save("catteam1");
        catteamsLoader.load();
    }
}
