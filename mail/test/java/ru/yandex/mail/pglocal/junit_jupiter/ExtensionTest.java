package ru.yandex.mail.pglocal.junit_jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.pglocal.Database;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PgLocalExtension.class)
class ExtensionTest {
    private static final String MIGRATIONS = "migrations";

    private static Database database;

    private static int tableSize(Database db) {
        return db.fetch("SELECT COUNT(*) FROM users", ResultSet::getFetchSize);
    }

    @BeforeAll
    public static void init(@InitDb(migration = MIGRATIONS, name = "global") Database database) {
        ExtensionTest.database = database;
    }

    @Test
    @DisplayName("Verify extension parameter resolver injects configured database object")
    void testParameterResolver(@InitDb(migration = MIGRATIONS, name = "local") Database db) {
        assertEquals(tableSize(db), 0);
        db.execute("INSERT INTO users VALUES (DEFAULT, 'someName', 42)");
    }

    @Test
    @DisplayName("Verify extension global parameter resolver injects configured database object")
    void testGlobalParameterResolver() {
        assertEquals(tableSize(database), 0);
        database.execute("INSERT INTO users VALUES (DEFAULT, 'global', 8)");
    }
}
