package ru.yandex.market.billing.fulfillment.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
class CheckpointUpdateImportDateCommandTest extends FunctionalTest {

    @Autowired
    private CheckpointUpdateImportDateCommand command;

    @Mock
    private Terminal terminal;

    @BeforeEach
    void setup() {
        PrintWriter printWriter = new PrintWriter(new StringWriter());
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @Test
    @DbUnitDataSet(
            before = "CheckpointUpdateImportDateCommandTest.testUpdateImportDate.before.csv",
            after = "CheckpointUpdateImportDateCommandTest.testUpdateImportDate.after.csv"
    )
    void testUpdateImportDate() {
        String[] strings = new String[0];
        command.executeCommand(new CommandInvocation(command.getNames()[0], strings, Map.of()), terminal);
    }
}
