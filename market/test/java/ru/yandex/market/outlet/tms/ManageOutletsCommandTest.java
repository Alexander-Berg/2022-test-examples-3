package ru.yandex.market.outlet.tms;

import java.io.PrintWriter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;
import static ru.yandex.market.outlet.tms.ManageOutletsCommand.COMMAND_NAME;

/**
 * Функциональные тесты для {@link ManageOutletsCommand}
 */
class ManageOutletsCommandTest extends FunctionalTest {

    @Autowired
    private ManageOutletsCommand command;

    @Autowired
    private Terminal terminal;

    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "ManageOutletsCommandTest.testDeleteOutlets.before.csv",
            after = "ManageOutletsCommandTest.testDeleteOutlets.after.csv")
    void testDeleteOutlets() {
        runCommand("delete", "1", "3", "20");
    }

    @Test
    @DbUnitDataSet(
            before = "ManageOutletsCommandTest.testSetOutletFields.before.csv",
            after = "ManageOutletsCommandTest.testSetOutletFields.after.csv")
    void testSetOutletFields() {
        runCommand("set", "1", "delivery_service_id=");
        runCommand("set", "2", "deleted=1", "delivery_service_id=222");
        runCommand("set", "3", "nesu_last_sync_time=");
        runCommand("set", "4", "nesu_pickup_point_id=456");
    }

    @Test
    @DbUnitDataSet(
            before = "ManageOutletsCommandTest.testSetAvailabilityByDs.before.csv",
            after = "ManageOutletsCommandTest.testSetAvailabilityByDs.after.csv")
    void testSetAvailabilityByDs() {
        runCommand("set-availability-by-ds", "1", "outlet", "false");
    }

    private void runCommand(final String... arguments) {
        command.executeCommand(new CommandInvocation(COMMAND_NAME, arguments, Map.of()), terminal);
    }

}
