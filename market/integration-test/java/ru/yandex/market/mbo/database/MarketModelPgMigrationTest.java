package ru.yandex.market.mbo.database;

public class MarketModelPgMigrationTest extends BaseMigrationTest {

    @Override
    public String getPathToChangelog() {
        return "mbo-db/market_model_pg.changelog.xml";
    }
}
