package ru.yandex.market.commands;

import java.io.PrintWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.commands.FeatureStateInfoCommand.COMMAND_NAME;

/**
 * Тесты для {@link FeatureStateInfoCommand}
 */
class FeatureStateInfoCommandTest extends FunctionalTest {

    @Autowired
    private FeatureStateInfoCommand command;

    private Terminal terminal;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        terminal = mock(Terminal.class);
        printWriter = mock(PrintWriter.class);
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "FeatureStateInfoCommand/FeatureStateInfoCommand.testCommandFullOutput.before.csv"
    )
    void testCommandFullOutput() {
        String expected = "shop_id: 1\n" +
                "feature: MARKETPLACE_SELF_DELIVERY(1015)\n" +
                "status: FAIL\n" +
                "open cutoffs: \n" +
                "\tORDER_NOT_ACCEPTED(107)\n" +
                "\tQUALITY_OTHER(109)\n" +
                "\tPINGER(1007)\n" +
                "open cutoffs by statuses:\n" +
                "\tSUCCESS: ORDER_NOT_ACCEPTED(107)\n" +
                "\tFAIL: QUALITY_OTHER(109)\n" +
                "\tWITHOUT STATUS: PINGER(1007)\n" +
                "feature related moderation: \n" +
                "\tCPA_PREMODERATION(3): \n" +
                "\t\tstart date: 2020-01-01 03:01:00.0\n" +
                "\t\tupdate date: -\n" +
                "\t\tstatus: CHECKING(4)\n" +
                "\tSELF_CHECK(5): no\n" +
                "\tDSBS_LITE_CHECK(7): no";
        executeCommand(expected);
    }

    @Test
    @DbUnitDataSet(
            before = "FeatureStateInfoCommand/FeatureStateInfoCommand.testCommandMinOutput.before.csv"
    )
    void testCommandMinOutput() {
        String expected = "shop_id: 1\n" +
                "feature: MARKETPLACE_SELF_DELIVERY(1015)\n" +
                "status: SUCCESS\n" +
                "open cutoffs: no\n" +
                "feature related moderation: \n" +
                "\tCPA_PREMODERATION(3): no\n" +
                "\tSELF_CHECK(5): no\n" +
                "\tDSBS_LITE_CHECK(7): no";
        executeCommand(expected);
    }

    private void executeCommand(String expected) {
        var commandInvocation = new CommandInvocation(COMMAND_NAME,
                new String[]{"1", "MARKETPLACE_SELF_DELIVERY"}, null);
        command.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(argumentCaptor.capture());
        System.out.println(argumentCaptor.getValue());
        assertThat(argumentCaptor.getValue(), is(expected));
    }
}
