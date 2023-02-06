package ru.yandex.market.commands;

import java.io.PrintWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplacePartnerOgrnCommandTest extends FunctionalTest {

    private CommandInvocation commandInvocation;

    private Terminal terminal;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private ReplacePartnerOgrnCommand command;

    @BeforeEach
    void setUp() {
        commandInvocation = mock(CommandInvocation.class);
        when(commandInvocation.getArgumentsCount()).thenReturn(2);
        when(commandInvocation.getArgument(eq(0))).thenReturn("5");
        when(commandInvocation.getArgument(eq(1))).thenReturn("5555");

        terminal = mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(mock(PrintWriter.class));

        command = new ReplacePartnerOgrnCommand(namedParameterJdbcTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "ReplacePartnerOgrnCommandTest.before.csv",
            after = "ReplacePartnerOgrnCommandTest.after.csv"
    )
    void doJob() {
        command.executeCommand(commandInvocation, terminal);
    }
}
