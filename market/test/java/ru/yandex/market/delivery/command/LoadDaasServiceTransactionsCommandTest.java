package ru.yandex.market.delivery.command;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.core.delivery.service.DaasServiceTransactionSyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadDaasServiceTransactionsCommandTest {
    private static final String COMMAND_NAME = "load-daas-service-transactions";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final String TO_DATE_OPTION = "to-date";
    private static final ImmutableMap<String, String> VALID_PARAMS = ImmutableMap.<String, String>builder()
            .put(FROM_DATE_OPTION, "2019-01-01")
            .put(TO_DATE_OPTION, "2019-01-02")
            .build();

    @Mock
    private DaasServiceTransactionSyncService transactionSyncService;

    @Mock
    private Terminal terminal;

    private LoadDaasServiceTransactionsCommand command;

    private static Stream<Arguments> getNegativeData() {
        return Stream.of(
                Arguments.of(
                        commandInvocation(params -> params.put(FROM_DATE_OPTION, "test")),
                        "java.time.format.DateTimeParseException: Text 'test' could not be parsed at index 0"
                ),
                Arguments.of(
                        commandInvocation(params -> params.put(FROM_DATE_OPTION, null)),
                        "Required option from-date is empty"
                ),
                Arguments.of(
                        commandInvocation(params -> params.put(TO_DATE_OPTION, "test")),
                        "java.time.format.DateTimeParseException: Text 'test' could not be parsed at index 0"
                ),
                Arguments.of(
                        commandInvocation(params -> params.put(TO_DATE_OPTION, null)),
                        "Required option to-date is empty"
                )
        );
    }

    private static CommandInvocation commandInvocation(Consumer<Map<String, String>> paramsChanger) {
        final Map<String, String> params = new HashMap<>(VALID_PARAMS);
        paramsChanger.accept(params);
        return new CommandInvocation(COMMAND_NAME, new String[]{}, params);
    }

    @BeforeEach
    void init() {
        command = new LoadDaasServiceTransactionsCommand(transactionSyncService);
        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @Test
    @DisplayName("Успешные кейсы выполнения команды")
    void executeCommandSuccessfully() {
        command.executeCommand(commandInvocation(params -> {}), terminal);

        verify(transactionSyncService).syncTransactions(
                LocalDateTime.of(2019, 1, 1, 0, 0, 0),
                LocalDateTime.of(2019, 1, 2, 23, 59, 59, 999999999)
        );

        verifyNoMoreInteractions(transactionSyncService);
    }

    @ParameterizedTest
    @DisplayName("Негативные кейсы выполнения команды")
    @MethodSource("getNegativeData")
    void executeCommandWithError(CommandInvocation commandInvocation, String error) {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> command.executeCommand(commandInvocation, terminal)
        );
        assertEquals(error, exception.getMessage());
        verifyZeroInteractions(transactionSyncService);
    }
}
