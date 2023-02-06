package ru.yandex.market.mbo.database;

public class MboIrTmsPgMigrationTest extends BaseMigrationTest {

    @Override
    public String getPathToChangelog() {
        return "mbo-db/mbo_ir_tms_pg.changelog.xml";
    }
}
