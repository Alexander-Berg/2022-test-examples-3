package ru.yandex.market.supplier.command;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "ManageDeliveryHolidaysCommandTest.before.csv")
class ManageDeliveryHolidaysCommandTest extends FunctionalTest {

    private static final String CMD_NAME = "manage-delivery-holidays";

    @Autowired
    private ManageDeliveryHolidaysCommand command;


    @Test
    @DbUnitDataSet(after = "ManageDeliveryHolidaysCommandTest.addHolidays.after.csv")
    void testAddHolidays() {
        executeCommand("add-holidays", "10313723,10313340", "20.12.3020,15.01.3021");
    }

    @Test
    @DbUnitDataSet(after = "ManageDeliveryHolidaysCommandTest.resetHolidays.after.csv")
    void testResetHolidays() {
        executeCommand("reset-holidays", "10313723");
    }

    @Test
    @DbUnitDataSet(after = "ManageDeliveryHolidaysCommandTest.setAvailableOnHolidays.after.csv")
    void testSetAvailableOnHolidays() {
        executeCommand("set-available-on-holidays", "10313723", "true");
    }

    @Test
    void testUsages() {
        assertNotNull(command.getUsage());
    }

    private void executeCommand(final String... args) {
        final Terminal terminal = Mockito.mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));

        command.executeCommand(new CommandInvocation(CMD_NAME, args, Collections.emptyMap()), terminal);
    }

}
