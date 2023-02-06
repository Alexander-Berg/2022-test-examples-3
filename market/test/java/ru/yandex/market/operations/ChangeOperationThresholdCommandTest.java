package ru.yandex.market.operations;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.core.operations.OperationService;
import ru.yandex.market.core.operations.model.OperationType;
import ru.yandex.market.core.operations.model.OperationTypeInfo;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Функциональные тесты на {@link ChangeOperationThresholdCommand}.
 *
 * @author sherafgan
 */
class ChangeOperationThresholdCommandTest extends FunctionalTest {

    @Autowired
    private OperationService operationService;

    @Autowired
    private ChangeOperationThresholdCommand changeOperationThresholdCommand;

    private PrintWriter writer = Mockito.mock(PrintWriter.class);

    @Test
    void executeCommandNewThreshold_ok() {
        Optional<OperationTypeInfo> typeInfo = operationService.getOperationTypeInfo(OperationType.DEFAULT_PULL_OPERATION);
        Assertions.assertTrue(typeInfo.isPresent());
        OperationTypeInfo operationTypeInfo = typeInfo.get();
        Assertions.assertEquals(0L, operationTypeInfo.getId());
        Assertions.assertEquals("DEFAULT_PULL_OPERATION", operationTypeInfo.getName());
        Assertions.assertEquals("Стандартная pull операция", operationTypeInfo.getDescription());
        Assertions.assertEquals(3600, operationTypeInfo.getTreshold());
        Assertions.assertFalse(operationTypeInfo.isPush());
        executeCommand("0", "5400");
        typeInfo = operationService.getOperationTypeInfo(OperationType.DEFAULT_PULL_OPERATION);
        operationTypeInfo = typeInfo.get();
        Assertions.assertEquals(5400, operationTypeInfo.getTreshold());

    }

    @Test
    void executeCommandNewNegativeThreshold_fail() {
        Mockito.doNothing().when(writer).println(Mockito.anyString());
        executeCommand("0", "-3600");
        Mockito.verify(writer, Mockito.times(1))
                .println(Mockito.contains("BAD REQUEST: Error while parsing arguments"));
    }

    private void executeCommand(final String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();
        changeOperationThresholdCommand.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(writer);
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

    private CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("change-op-threshold", args, Collections.emptyMap());
    }
}
