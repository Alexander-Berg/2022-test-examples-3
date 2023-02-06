package ru.yandex.market.billing.ora2pg.tasks;

import java.time.LocalDate;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
class OibaTransferTaskTest extends FunctionalTest {

    @Autowired
    OibaTransferTask oibaTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Test
    void testSpringInitialization() {
        assertThat(oibaTransferTask.getName()).isEqualTo("oiba");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(oibaTransferTask)).hasMessageContaining("oiba");
    }

    @Test
    void testShortInterval() {
        // Интервал дат меньше одного месяца
        var from = LocalDate.parse("2021-07-01");
        var to = LocalDate.parse("2021-07-15");
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());

        OibaTransferTask task = createMockedTask();
        task.execute(taskInput, null);

        verify(task).execute(any(JsonNode.class), isNull());
        verify(task).doIteration(eq(from), eq(to));
        verifyNoMoreInteractions(task);
    }

    @Test
    void testLongInterval() {
        // Интервал дат больше одного месяца
        var from = LocalDate.parse("2021-01-02");
        var to = LocalDate.parse("2021-04-01");
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());

        OibaTransferTask task = createMockedTask();
        task.execute(taskInput, null);

        verify(task).execute(any(JsonNode.class), isNull());
        verify(task).doIteration(eq(from), eq(LocalDate.parse("2021-02-02")));

        verifyNoMoreInteractions(task);
    }

    @Test
    void testLongInterval_full() {
        // Интервал дат больше одного месяца
        var from = "2021-01-02";
        var to = "2021-04-01";
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from);
        taskInput.put("to", to);

        OibaTransferTask task = createMockedTask();
        transferOraToPgCommand.registerTask(task);

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, task.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );

        transferOraToPgCommand.unregisterTask(task);

        verify(task).execute(any(JsonNode.class), isNull());
        verify(task, times(2)).execute(any(JsonNode.class), any(JsonNode.class));

        verify(task).doIteration(
                eq(LocalDate.parse(from)), eq(LocalDate.parse("2021-02-02"))
        );
        verify(task).doIteration(
                eq(LocalDate.parse("2021-02-02")), eq(LocalDate.parse("2021-03-02"))
        );
        verify(task).doIteration(
                eq(LocalDate.parse("2021-03-02")), eq(LocalDate.parse(to))
        );

        verify(task, atLeastOnce()).getName();
        verifyNoMoreInteractions(task);
    }

    private OibaTransferTask createMockedTask() {
        var task = Mockito.mock(OibaTransferTask.class);
        doCallRealMethod().when(task).execute(any(), any());
        when(task.getName()).thenReturn("test-oiba");
        return task;
    }
}
