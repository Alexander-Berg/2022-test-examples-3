package ru.yandex.market.mbo.database;

import java.util.List;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.PreparedDbRule;
import liquibase.Contexts;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class BaseMigrationTest {

    @Rule
    public PreparedDbRule db = EmbeddedPostgresRules.preparedDatabase(
        LiquibasePreparer.forClasspathLocation(getPathToChangelog(), new Contexts("tests_only"))
    );

    public abstract String getPathToChangelog();

    @Test
    public void testSelectOne() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(db.getTestDatabase());

        final List<Long> result = jdbcTemplate.query("SELECT 1", (rs, rowNum) -> rs.getLong(1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, (long) result.get(0));
    }
}
