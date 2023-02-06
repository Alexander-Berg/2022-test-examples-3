package ru.yandex.market.commands;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import java.io.PrintWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.commands.ToggleCrossdockFeatureCommand.COMMAND_NAME;

/**
 * Тесты для {@link ToggleCrossdockFeatureCommand}
 */
class ToggleCrossdockFeatureCommandTest extends FunctionalTest {
    @Autowired
    private ToggleCrossdockFeatureCommand command;

    private Terminal terminal;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        terminal = mock(Terminal.class);
        printWriter = mock(PrintWriter.class);
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @DisplayName("Включает кроссдок")
    @Test
    @DbUnitDataSet(
        before = "ToggleCrossdockFeatureCommand/TestOnCrossdock.before.csv",
        after = "ToggleCrossdockFeatureCommand/TestOnCrossdock.after.csv"
    )
    void activateCrossdock() {
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{"304", "ON"}, null), terminal);
    }

    @DisplayName("Отключает кроссдок")
    @Test
    @DbUnitDataSet(
            before = "ToggleCrossdockFeatureCommand/TestOffCrossdock.before.csv",
            after = "ToggleCrossdockFeatureCommand/TestOffCrossdock.after.csv"
    )
    void disableCrossdock() {
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{"305", "OFF"}, null), terminal);
    }
}
