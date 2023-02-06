package ru.yandex.market.communication.proxy.tms.command;

import java.io.PrintWriter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;

import static org.mockito.ArgumentMatchers.argThat;

class EnvironmentCommandTest extends AbstractCommunicationProxyTest {

    @Autowired
    private EnvironmentCommand environmentCommand;

    @Test
    @DbUnitDataSet(before = "EnvironmentCommand.before.csv")
    void testValueGet() {
        CommandInvocation invocation = Mockito.mock(CommandInvocation.class);
        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"get", "environment.test.first.name"}
        );
        Terminal terminal = Mockito.mock(Terminal.class);
        PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(terminal.getWriter()).thenReturn(writer);

        environmentCommand.execute(invocation, terminal);

        Mockito.verify(writer).println(List.of("400"));
    }

    @Test
    void testValueSet() {
        CommandInvocation invocation = Mockito.mock(CommandInvocation.class);
        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"set", "environment.test.first.name", "test.value"}
        );
        Terminal terminal = Mockito.mock(Terminal.class);
        PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(terminal.getWriter()).thenReturn(writer);

        environmentCommand.execute(invocation, terminal);

        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"get", "environment.test.first.name"}
        );

        environmentCommand.execute(invocation, terminal);

        Mockito.verify(writer).println(List.of("test.value"));
    }

    @Test
    void testValueAdd() {
        CommandInvocation invocation = Mockito.mock(CommandInvocation.class);
        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"add", "environment.test.test", "1", "2", "3"}
        );
        Terminal terminal = Mockito.mock(Terminal.class);
        PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(terminal.getWriter()).thenReturn(writer);

        environmentCommand.execute(invocation, terminal);

        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"get", "environment.test.test"}
        );

        environmentCommand.execute(invocation, terminal);

        Mockito.verify(writer).println((Object) argThat(arg -> {
            if (!(arg instanceof List)) {
                return false;
            }
            List listArg = (List) arg;
            return listArg.contains("1") && listArg.contains("2") && listArg.contains("3") && listArg.size() == 3;
        }));
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentCommand.before.csv",
            after = "EnvironmentCommand.testValueRemove.after.csv"
    )
    public void testValueRemove() {
        CommandInvocation invocation = Mockito.mock(CommandInvocation.class);
        Mockito.when(invocation.getArguments()).thenReturn(
                new String[]{"remove", "environment.test.first.name", "400"}
        );

        Terminal terminal = Mockito.mock(Terminal.class);
        PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(terminal.getWriter()).thenReturn(writer);

        environmentCommand.execute(invocation, terminal);
    }
}
