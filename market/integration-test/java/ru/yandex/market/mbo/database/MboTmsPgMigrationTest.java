package ru.yandex.market.mbo.database;

public class MboTmsPgMigrationTest extends BaseMigrationTest {

    @Override
    public String getPathToChangelog() {
        return "mbo-db/mbo_tms_pg.changelog.xml";
    }
}
