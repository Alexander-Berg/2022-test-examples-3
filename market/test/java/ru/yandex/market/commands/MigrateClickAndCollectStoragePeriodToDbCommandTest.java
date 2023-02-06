package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.TestClock;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

public class MigrateClickAndCollectStoragePeriodToDbCommandTest extends FunctionalTest {

    private final Terminal terminal;
    private final PrintWriter printWriter;

    @Autowired
    private MigrateClickAndCollectStoragePeriodToDbCommand command;

    @Autowired
    private TestClock clock;

    public MigrateClickAndCollectStoragePeriodToDbCommandTest() {
        this.terminal = Mockito.mock(Terminal.class);
        this.printWriter = Mockito.mock(PrintWriter.class);
    }

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2021-07-02T00:00:00.00Z"));
    }

    @Test
    @DbUnitDataSet(
            before = "MigrateClickAndCollectStoragePeriodToDbCommandTest.before.csv",
            after = "MigrateClickAndCollectStoragePeriodToDbCommandTest.after.csv"
    )
    public void testSuccess() {
        CommandInvocation commandInvocation = new CommandInvocation("migrate-cc-storage-period",
                new String[]{},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "MigrateClickAndCollectStoragePeriodToDbCommandTest.before.csv",
            after = "MigrateClickAndCollectStoragePeriodToDbCommandSelectPartnersTest.after.csv"
    )
    public void testSuccessSelectPartners() {
        CommandInvocation commandInvocation = new CommandInvocation("migrate-cc-storage-period",
                new String[]{"2"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }
}
