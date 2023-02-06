package ru.yandex.market.deepdive.liquibase;


import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.opentable.db.postgres.embedded.LiquibasePreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmbeddedPostgresTest {

    @Rule
    public PreparedDbRule db =
            EmbeddedPostgresRules.preparedDatabase(LiquibasePreparer.forClasspathLocation("liquibase/pg.schema.xml"))
                    .customize(builder -> {
                        if ("Mac OS X".equals(System.getProperty("os.name"))) {
                            builder.setPgDirectoryResolver(s -> new File("/tmp/postgresql-12.9-1-osx-binaries/pgsql/"));
                        }
                    });

    @Test
    @Ignore
    public void testTablesMade() throws Exception {
        try (Connection c = db.getTestDatabase().getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM spok");
            rs.next();
            assertEquals("test liquibase", rs.getString(1));
        }
    }
}
