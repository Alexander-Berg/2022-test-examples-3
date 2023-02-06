package ru.yandex.market.mbo.db.pg;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.PreparedDbRule;
import liquibase.Contexts;
import org.junit.Rule;

/**
 * Copied from mbo-db.
 */
public abstract class BasePgMigrationTest {

    @Rule
    public PreparedDbRule db = EmbeddedPostgresRules.preparedDatabase(
        LiquibasePreparer.forClasspathLocation(getPathToChangelog(), new Contexts("tests_only"))
    );

    public abstract String getPathToChangelog();
}
