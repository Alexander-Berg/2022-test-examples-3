package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

public class SetOutletDefaultStoragePeriodCommandTest extends FunctionalTest {

    private final Terminal terminal;
    private final PrintWriter printWriter;

    @Autowired
    private SetOutletDefaultStoragePeriodCommand setOutletDefaultStoragePeriodCommand;

    public SetOutletDefaultStoragePeriodCommandTest() {
        this.terminal = Mockito.mock(Terminal.class);
        this.printWriter = Mockito.mock(PrintWriter.class);
    }

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "SetOutletDefaultStoragePeriodCommandTest.before.csv",
            after = "SetOutletDefaultStoragePeriodCommandTest.after.csv"
    )
    public void testSuccess() {
        CommandInvocation commandInvocation = new CommandInvocation("set-outlet-default-storage-period",
                new String[]{},
                Collections.emptyMap());

        setOutletDefaultStoragePeriodCommand.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "SetOutletDefaultStoragePeriodCommandTestCC.before.csv",
            after = "SetOutletDefaultStoragePeriodCommandTestCC.after.csv"
    )
    public void testClickAndCollect() {
        CommandInvocation commandInvocation = new CommandInvocation("set-outlet-default-storage-period",
                new String[]{"CLICK_AND_COLLECT", "7"},
                Collections.emptyMap());

        setOutletDefaultStoragePeriodCommand.executeCommand(commandInvocation, terminal);
    }

}
