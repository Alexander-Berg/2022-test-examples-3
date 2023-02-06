package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тест для {@link ImportClientToAbcSyncEventCommand}.
 * Проверяет выполнение команды в зависимости от переданных параметров.
 */
class ImportClientToAbcSyncEventCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private ImportClientToAbcSyncEventCommand command;

    @BeforeEach
    void setup() {
        terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DisplayName("команда на добавление всех клиентов для привязки в ABC")
    @DbUnitDataSet(
            before = "abc/ImportClientToAbcSyncEventCommand.addAll.before.csv",
            after = "abc/ImportClientToAbcSyncEventCommand.addAll.after.csv"
    )
    void testExecuteCommandWithoutArguments() {
        CommandInvocation commandInvocation = new CommandInvocation(
                ImportClientToAbcSyncEventCommand.NAME,
                new String[]{},
                Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("команда на добавление клиинетов 1 и 2 для привязки в ABC")
    @DbUnitDataSet(
            after = "abc/ImportClientToAbcSyncEventCommand.addClients12.after.csv"
    )
    void testExecuteCommandWithArguments() {
        final String[] arguments = {"1", "2"};
        CommandInvocation commandInvocation = new CommandInvocation(
                ImportClientToAbcSyncEventCommand.NAME,
                arguments,
                Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("команда на добавление клиинета 1 для привязки в ABC")
    @DbUnitDataSet(
            after = "abc/ImportClientToAbcSyncEventCommand.addClients1.after.csv"
    )
    void testExecuteCommandWithOneArgument() {
        final String[] arguments = {"1"};
        CommandInvocation commandInvocation = new CommandInvocation(
                ImportClientToAbcSyncEventCommand.NAME,
                arguments,
                Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("команда на очистку всех событий привязки клиентов в ABC")
    @DbUnitDataSet(
            before = "abc/ImportClientToAbcSyncEventCommand.remove.before.csv",
            after = "abc/ImportClientToAbcSyncEventCommand.remove.after.csv"
    )
    void testExecuteCommandWithOneArgumentClear() {
        final String[] arguments = {"clear"};
        CommandInvocation commandInvocation = new CommandInvocation(
                ImportClientToAbcSyncEventCommand.NAME,
                arguments,
                Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);
    }

}
