package ru.yandex.market.billing.checkout.reprocess;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = {"db/datasource.csv"})
class ProcessIgnoredOrdersCommandTest extends FunctionalTest {

    @Autowired
    private ProcessIgnoredOrdersCommand command;

    @Mock
    private Terminal terminal;

    private StringWriter terminalBuffer;

    @BeforeEach
    void setup() {
        this.terminalBuffer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(terminalBuffer);
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @Test
    @DisplayName("Сохраняем заигноренные события командой")
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessEvents.before.xml",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessEvents.after.xml",
            type = DataSetType.XML
    )
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessEvents.before.csv",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessEvents.after.csv"
    )
    void testProcessEvents() {
        String[] strings = {"1"};
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);

        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages)
                .contains("Processing from holding 1 orders")
                .contains("Processing 1 events for order id 1")
                .contains("Successfully processed 1 events")
                .contains("Processed and delete from holding 1 orders")
                .matches(".+\n.+\n.+\n.+\n");
    }

    @Test
    @DisplayName("Обрабатываем заказ без заигноренных событий")
    void testDoNotHaveAnyEvents() {
        String[] strings = {"1", "12345"};
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);

        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages)
                .contains("WARN! There isn't any events for order id 1")
                .contains("WARN! There isn't any events for order id 12345");
    }

    @Test
    @DisplayName("В случае ошибки при обработке состояние заигноренных таблиц не должно измениться")
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessBadEvents.before.xml",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessBadEvents.before.xml",
            type = DataSetType.XML
    )
    void testExceptionInProcessing() {
        String[] strings = {"1"};
        Assertions.assertThatThrownBy(() ->
                command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Обрабатываем все скрытые заказы")
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.before.xml",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.after.xml",
            type = DataSetType.XML
    )
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.before.csv",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.after.csv"
    )
    void testProcessAllOrders() {
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        new String[0],
                        Map.of("all", "true")),
                terminal
        );
    }

    @Test
    @DisplayName("Обрабатываем все скрытые заказы и не падаем при ошибки")
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrdersWithException.before.xml",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrdersWithException.after.xml",
            type = DataSetType.XML
    )
    @DbUnitDataSet(
            before = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.before.csv",
            after = "db/ProcessIgnoredOrdersCommandTest.testProcessAllOrders.after.csv"
    )
    void testProcessAllOrdersWithException() {
        Assertions.assertThatCode(() ->
                command.executeCommand(
                        new CommandInvocation(
                                command.getNames()[0],
                                new String[0],
                                Map.of("all", "true")),
                        terminal
                )
        ).isExactlyInstanceOf(RuntimeException.class);

        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages)
                .contains("Processing order id 2 thrown exception")
                .contains("Processed and delete from holding 1 orders");
    }
}
