package ru.yandex.market.outlet;

import java.io.PrintWriter;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link CopyOutletCommand}
 */
class CopyOutletCommandTest extends FunctionalTest {

    @Autowired
    private CopyOutletCommand copyOutletCommand;

    private Terminal terminal;

    @BeforeEach
    void setUp() {
        terminal = Mockito.mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        when(terminal.areYouSure()).thenReturn(true);
    }

    @DbUnitDataSet(before = "CopyOutletCommand.before.csv", after = "CopyOutletCommand.after.csv")
    @Test
    void testCopyAllOutlets() {
        copyOutletCommand.executeCommand(createCommand("1", "2"), terminal);
    }

    @DbUnitDataSet(before = "CopyOutletCommand.before.csv", after = "CopySpecifiedOutletCommand.after.csv")
    @Test
    void testCopySpecifiedOutlets() {
        copyOutletCommand.executeCommand(createCommand("1", "2", "20"), terminal);
    }

    @DbUnitDataSet(before = "CopyAllWithNewTypeOutletCommand.before.csv",
            after = "CopyAllWithNewTypeOutletCommand.after.csv")
    @Test
    void testCopyAllWithNewType() {
        copyOutletCommand.executeCommand(createCommand("1", "2", "RETAIL"), terminal);
    }

    @DbUnitDataSet(before = "CopySpecifiedWithNewType.before.csv", after = "CopySpecifiedWithNewType.after.csv")
    @Test
    void testCopySpecifiedWithNewType() {
        copyOutletCommand.executeCommand(createCommand("1", "2", "10", "20:RETAIL", "30:MIXED"), terminal);
    }

    @Nonnull
    private CommandInvocation createCommand(String... arguments) {
        return new CommandInvocation(
                "copy-outlets",
                arguments,
                Collections.emptyMap()
        );
    }
}
