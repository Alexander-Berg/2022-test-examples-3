package ru.yandex.market.mbo.database;

public class SiteCatalogPgMigrationTest extends BaseMigrationTest {

    @Override
    public String getPathToChangelog() {
        return "mbo-db/site_catalog_pg.changelog.xml";
    }
}
