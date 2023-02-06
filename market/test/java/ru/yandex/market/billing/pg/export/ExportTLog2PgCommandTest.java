package ru.yandex.market.billing.pg.export;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class ExportTLog2PgCommandTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;
    @Mock
    private Oracle2PgExportService exportService;
    @Mock
    private Terminal terminal;

    private ExportTLog2PgCommand command;

    @BeforeEach
    void setup() {
        StringWriter terminalBuffer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(terminalBuffer);
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);

        this.command = new ExportTLog2PgCommand(
                exportService, environmentService
        );
    }

    @Test
    void testBatchExport() {
        String[] strings = {"100"};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        Map.of("batch", "")
                ),
                terminal
        );

        Mockito.verify(exportService)
                .exportToPg(
                        any(),
                        argThat(filter -> {
                            Assertions.assertThat(filter)
                                    .hasToString("(transaction_id >= :tranIdFrom " +
                                            "AND transaction_id < :tranIdTo)");
                            Assertions.assertThat(filter.toParameters())
                                    .containsEntry("tranIdFrom", 0L)
                                    .containsEntry("tranIdTo", 100L);
                            return true;
                        })
                );
    }

    @DbUnitDataSet(
            before = "db/ExportTLog2PgCommandTest.testBatchExportWithEnv.before.csv",
            after = "db/ExportTLog2PgCommandTest.testBatchExportWithEnv.after.csv"
    )
    @Test
    void testBatchExportWithEnv() {
        String[] strings = {"100"};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        Map.of("batch", "")
                ),
                terminal
        );

        Mockito.verify(exportService)
                .exportToPg(
                        any(),
                        argThat(filter -> {
                            Assertions.assertThat(filter)
                                    .hasToString("(transaction_id >= :tranIdFrom " +
                                            "AND transaction_id < :tranIdTo)");
                            Assertions.assertThat(filter.toParameters())
                                    .containsEntry("tranIdFrom", 1000L)
                                    .containsEntry("tranIdTo", 1100L);
                            return true;
                        })
                );
    }

    @Test
    void testExportWithInterval() {
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        new String[0],
                        Map.of("id-from", "1000", "id-to", "2000")
                ),
                terminal
        );

        Mockito.verify(exportService)
                .exportToPg(
                        any(),
                        argThat(filter -> {
                            Assertions.assertThat(filter)
                                    .hasToString("(transaction_id >= :tranIdFrom " +
                                            "AND transaction_id < :tranIdTo)");
                            Assertions.assertThat(filter.toParameters())
                                    .containsEntry("tranIdFrom", 1000L)
                                    .containsEntry("tranIdTo", 2000L);
                            return true;
                        })
                );
    }

    @Test
    void testInvalidInterval() {
        Assertions.assertThatCode(
                () -> command.executeCommand(
                        new CommandInvocation(
                                command.getNames()[0],
                                new String[0],
                                Map.of("id-from", "10", "id-to", "1")
                        ),
                        terminal
                )
        ).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Option id-from must be less id-to");
    }

    @Test
    void testBatchExports() {
        int batchSize = 1_500_000;
        String[] strings = {Integer.toString(batchSize)};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        Map.of("batch", "")
                ),
                terminal
        );

        Mockito.verify(exportService, Mockito.times(3))
                .exportToPg(
                        any(),
                        argThat(filter -> {
                            Assertions.assertThat(filter)
                                    .hasToString("(transaction_id >= :tranIdFrom " +
                                            "AND transaction_id < :tranIdTo)");
                            return true;
                        })
                );
    }

    @Test
    void testIntervalExports() {
        String[] strings = {""};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        Map.of("id-from", "1", "id-to", "1500001")
                ),
                terminal
        );

        Mockito.verify(exportService, Mockito.times(3))
                .exportToPg(
                        any(),
                        argThat(filter -> {
                            Assertions.assertThat(filter)
                                    .hasToString("(transaction_id >= :tranIdFrom " +
                                            "AND transaction_id < :tranIdTo)");
                            return true;
                        })
                );
    }

    @ParameterizedTest
    @MethodSource("getInvalidBatchSizeArgs")
    void testInvalidBatchSize(
            String batchValue,
            String errorMsg
    ) {
        String[] args = {batchValue};
        Assertions.assertThatCode(
                () -> command.executeCommand(
                        new CommandInvocation(
                                command.getNames()[0],
                                args,
                                Map.of("batch", "")
                        ),
                        terminal
                )
        ).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(errorMsg);
    }

    static Stream<Arguments> getInvalidBatchSizeArgs() {
        return Stream.of(
                Arguments.of("1a1", "Batch size must be numerical"),
                Arguments.of("-1", "Batch size must have positive value, but was -1")
        );
    }
}
