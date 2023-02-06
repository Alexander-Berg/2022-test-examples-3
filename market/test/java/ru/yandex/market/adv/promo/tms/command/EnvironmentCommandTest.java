package ru.yandex.market.adv.promo.tms.command;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.adv.promo.tms.command.EnvironmentCommand.COMMAND_NAME;

public class EnvironmentCommandTest extends FunctionalTest {

    @Autowired
    private EnvironmentCommand command;

    @Autowired
    private Terminal terminal;
    private StringWriter terminalWriter;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        Mockito.when(terminal.getWriter()).thenReturn(Mockito.spy(new PrintWriter(terminalWriter)));
    }

    @Test
    @DbUnitDataSet
    void putSetting_setCorrectValue() {
        String[] args = {"set", "setting", "123"};

        String settingName = args[1];
        String oldValue = "not set";
        String newValue = args[2];
        Mockito.when(terminal.confirm(
                String.format("Setting: %s, old value: %s, new value: %s. " +
                              "Are you sure you want to change the value? (yes/no)",
                        settingName, oldValue, newValue)))
                .thenReturn(true);

        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, args, null);
        command.executeCommand(commandInvocation, terminal);

        String expectedTerminalData = "New value: 123";
        Assertions.assertEquals(expectedTerminalData, StringUtils.trimToEmpty(terminalWriter.toString()));
    }
}
