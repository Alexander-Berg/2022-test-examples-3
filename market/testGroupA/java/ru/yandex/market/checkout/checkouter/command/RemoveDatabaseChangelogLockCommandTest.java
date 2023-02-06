package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.LongRowMapper;
import ru.yandex.common.util.terminal.Command;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveDatabaseChangelogLockCommandTest extends AbstractServicesTestBase {

    private static final String COUNT_ALL_SQL = "select count(*) from databasechangeloglock";
    private static final String COUNT_LOCKED_SQL = "select count(*) from databasechangeloglock where locked = true";

    @Autowired
    private Command removeDatabaseChangelogLockCommand;

    @BeforeEach
    public void setUp() {
        transactionTemplate.execute(ts -> {
            // Эту таблицу транкейтим только здесь.
            masterJdbcTemplate.execute("truncate table databasechangeloglock");
            return null;
        });
    }

    @Test
    public void hasDescription() {
        assertTrue(removeDatabaseChangelogLockCommand.getDescription().contains("DATABASECHANGELOGLOCK"));
    }

    @Test
    public void nothingToDo() throws UnsupportedEncodingException {
        // Arrange
        // Act
        final String log = executeAndGetLog();

        // Assert
        assertTrue(log.contains("Nothing to do. Returning..."), log);
        assertEquals(0, getRowsCount(COUNT_ALL_SQL));
        assertEquals(0, getRowsCount(COUNT_LOCKED_SQL));
    }

    @Test
    public void unlockOne() throws UnsupportedEncodingException {
        // Arrange
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.execute("insert into databasechangeloglock (id, locked) values (1, true) " +
                    "on conflict (id) do update set locked = excluded.locked");
            return null;
        });
        assertEquals(1, getRowsCount(COUNT_ALL_SQL));
        assertEquals(1, getRowsCount(COUNT_LOCKED_SQL));

        // Act
        final String log = executeAndGetLog();

        // Assert
        assertTrue(log.contains("Has been unlocked successfully. 1 rows updated"), log);
        assertEquals(1, getRowsCount(COUNT_ALL_SQL));
        assertEquals(0, getRowsCount(COUNT_LOCKED_SQL));
    }

    @Test
    public void deleteSeveral() throws UnsupportedEncodingException {
        // Arrange
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.execute("insert into databasechangeloglock (id, locked) " +
                    "values (1, true), (2, true), (3, true) " +
                    "on conflict (id) do update set locked = excluded.locked");
            return null;
        });
        assertEquals(3, getRowsCount(COUNT_ALL_SQL));
        assertEquals(3, getRowsCount(COUNT_LOCKED_SQL));

        // Act
        final String log = executeAndGetLog();

        // Assert
        assertTrue(log.contains("Has been cleaned successfully. 3 rows deleted"), log);
        assertEquals(0, getRowsCount(COUNT_ALL_SQL));
        assertEquals(0, getRowsCount(COUNT_LOCKED_SQL));
    }

    @Test
    public void deleteSeveralExceptUnlocked() throws UnsupportedEncodingException {
        // Arrange
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.execute("insert into databasechangeloglock (id, locked) " +
                    "values (1, false), (2, true), (3, true) " +
                    "on conflict (id) do update set locked = excluded.locked");
            return null;
        });
        assertEquals(3, getRowsCount(COUNT_ALL_SQL));
        assertEquals(2, getRowsCount(COUNT_LOCKED_SQL));

        // Act
        final String log = executeAndGetLog();

        // Assert
        assertTrue(log.contains("Has been cleaned successfully. 2 rows deleted"), log);
        assertEquals(1, getRowsCount(COUNT_ALL_SQL));
        assertEquals(0, getRowsCount(COUNT_LOCKED_SQL));
    }

    private String executeAndGetLog() throws UnsupportedEncodingException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Terminal terminal = createTerminal(output);

        removeDatabaseChangelogLockCommand.execute(createInvocation(), terminal);
        terminal.getWriter().flush();

        return output.toString(StandardCharsets.UTF_8.name());
    }

    private CommandInvocation createInvocation() {
        return new CommandInvocation(removeDatabaseChangelogLockCommand.getNames()[0],
                new String[]{}, Collections.emptyMap());
    }

    private Terminal createTerminal(final ByteArrayOutputStream output) {
        return new TestTerminal(new ByteArrayInputStream(new byte[0]), output);
    }

    private long getRowsCount(final String sqlQuery) {
        return masterJdbcTemplate.query(sqlQuery, new LongRowMapper())
                .stream()
                .findFirst()
                .orElse(0L);
    }
}
