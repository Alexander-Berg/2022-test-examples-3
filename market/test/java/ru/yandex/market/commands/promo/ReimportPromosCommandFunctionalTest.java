package ru.yandex.market.commands.promo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static ru.yandex.market.commands.promo.ReimportPromosCommand.COMMAND_NAME;
import static ru.yandex.market.commands.promo.ReimportPromosCommand.MIN_DATE;

public class ReimportPromosCommandFunctionalTest extends FunctionalTest {
    @Autowired
    private ReimportPromosCommand command;

    @Autowired
    private Terminal terminal;
    private StringWriter terminalWriter;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        Mockito.when(terminal.getWriter()).thenReturn(Mockito.spy(new PrintWriter(terminalWriter)));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
    }

    @Test
    @DbUnitDataSet(before = "updateAnaplanPromosTest.before.csv", after = "updateAnaplanPromosTest.after.csv")
    void updateAnaplanPromosTest() {
        Map<String, String> args = Map.of(
                MIN_DATE, "1970-01-01"
        );

        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[]{}, args);
        command.executeCommand(commandInvocation, terminal);

        String expectedTerminalData = "" +
                "Input data: \n" +
                "\t" + MIN_DATE + "=1970-01-01T00:00:00Z\n\n" +
                "Min date for Anaplan promos is updated";
        Assertions.assertEquals(expectedTerminalData, StringUtils.trimToEmpty(terminalWriter.toString()));
    }
}
