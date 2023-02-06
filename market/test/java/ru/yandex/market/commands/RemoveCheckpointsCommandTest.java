package ru.yandex.market.commands;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.CommandParser;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тест на {@link RemoveCheckpointCommand}
 */
public class RemoveCheckpointsCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void init() {
        when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @Autowired
    private RemoveCheckpointCommand removeCheckpointCommand;

    @Test
    @DbUnitDataSet(
            before = "RemoveCheckpointsCommandTest.before.csv",
            after = "RemoveCheckpointsCommandTest.after.csv"
    )
    void test() {
        var commandExecutor = new CommandExecutor();
        commandExecutor.registerCommand(removeCheckpointCommand);
        commandExecutor.setCommandParser(new CommandParser());

        var commandInvoke = "remove-checkpoint-command --checkpointType=175 --orderIds=51991797";

        var input = new ByteArrayInputStream(commandInvoke.getBytes(StandardCharsets.UTF_8));

        var terminal = new Terminal(input, System.out) {
            @Override
            protected void onStart() {
                printWriter.println("Start command ${command::class.java}");
            }

            @Override
            protected void onClose() {
                printWriter.println("End command ${command::class.java}");
            }
        };

        commandExecutor.executeCommand(
                input,
                terminal
        );
    }
}
