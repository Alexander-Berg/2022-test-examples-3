package ru.yandex.market.billing.ora2pg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_CONTINUE;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
class TransferOraToPgCommandTest extends FunctionalTest {

    @Autowired
    TransferOraToPgCommand command;

    @Test
    void testInstantiation() {
        assertThat(command.getNames()).containsExactly(TransferOraToPgCommand.TRANSFER_TO_PG);
    }

    @Test
    @DbUnitDataSet(after = "simple-task.csv")
    void testSimpleTask() {
        var task = new TransferOraToPgTask() {
            boolean executed = false;

            @Override
            public String getName() {
                return "test-transfer-task";
            }

            @Override
            public JsonNode execute(@Nullable JsonNode input, @Nullable JsonNode state) {
                executed = true;
                return null;
            }
        };
        command.registerTask(task);

        command.executeCommand(
                createCommandInvocation(
                        entry("task-name", task.getName())
                ),
                null
        );

        assertThat(task.executed).isTrue();
    }

    @Test
    @DbUnitDataSet(after = "task-with-input.csv")
    void testTaskWithInput() {
        var task = new TransferOraToPgTask() {
            boolean executed = false;
            String from;
            String to;
            List<Integer> ignoredIds;

            @Override
            public String getName() {
                return "test-task-with-input";
            }

            public JsonNode execute(@Nullable JsonNode input, @Nullable JsonNode state) {
                from = input.get("from").asText();
                to = input.get("to").asText();

                var ids = new ArrayList<Integer>();
                input.get("ignore-ids").elements()
                        .forEachRemaining(item -> ids.add(item.intValue()));
                ignoredIds = ids;

                executed = true;

                return null;
            }
        };
        command.registerTask(task);

        command.executeCommand(
                createCommandInvocation(
                        entry(TASK_NAME, task.getName()),
                        entry(TASK_INPUT, "{\"from\": \"забор\", \"to\": \"обед\", \"ignore-ids\": [100,500]}")
                ),
                null
        );

        assertThat(task)
                .hasFieldOrPropertyWithValue("executed", true)
                .hasFieldOrPropertyWithValue("from", "забор")
                .hasFieldOrPropertyWithValue("to", "обед")
                .extracting("ignoredIds", as(InstanceOfAssertFactories.LIST))
                .containsExactly(100, 500);
    }

    @Test
    @DbUnitDataSet(after = "iterative-task.csv")
    void testIterativeTask() {
        var task = new TestIterativeTask();
        command.registerTask(task);

        command.executeCommand(
                createCommandInvocation(
                        entry(TASK_NAME, task.getName())
                ),
                null
        );

        command.unregisterTask(task);

        assertThat(task)
                .hasFieldOrPropertyWithValue("iterNumber", 3)
                .hasFieldOrPropertyWithValue("executed", true);
    }

    @Test
    @DbUnitDataSet(after = "iterative-task-continuation.csv")
    void testIterativeTaskContinuation() {
        var task = new TestIterativeTask();
        command.registerTask(task);

        // имитируем ошибку на итерации #1
        task.setInterruptOnIteration(1);
        assertThatThrownBy(() -> command.executeCommand(
                createCommandInvocation(
                        entry(TASK_NAME, task.getName())
                ),
                null
        )).hasMessageEndingWith("1");
        assertThat(task)
                .hasFieldOrPropertyWithValue("iterNumber", 1)
                .hasFieldOrPropertyWithValue("executed", false);

        // имитируем продолжение работы команды, и на итерации #1 она больше не падает
        // но выставляем прерывание на итерации #0, чтобы убедиться, что задача не стартанула сначала, а продолжилась
        task.setInterruptOnIteration(0);
        command.executeCommand(
                createCommandInvocation(
                        entry(TASK_NAME, task.getName()),
                        entry(TASK_CONTINUE, "")
                ),
                null
        );

        command.unregisterTask(task);

        assertThat(task)
                .hasFieldOrPropertyWithValue("iterNumber", 3)
                .hasFieldOrPropertyWithValue("executed", true);
    }

    @SafeVarargs
    @NotNull
    private CommandInvocation createCommandInvocation(Map.Entry<String, String>... options) {
        return new CommandInvocation(
                TransferOraToPgCommand.TRANSFER_TO_PG,
                new String[]{},
                Map.ofEntries(options)
        );
    }

    private static class TestIterativeTask implements TransferOraToPgTask {
        static final String LAST_ITER_NUM = "last-iter-num";
        private boolean executed = false;
        private int iterNumber = -1;
        private int interruptionIteration = -1;

        @Override
        public String getName() {
            return "test-iterative-task";
        }

        public JsonNode execute(@Nullable JsonNode input, @Nullable JsonNode state) {
            iterNumber = (state == null) ? 0 : state.get(LAST_ITER_NUM).asInt() + 1;

            if (iterNumber == interruptionIteration) {
                throw new RuntimeException("Iteration failed: " + iterNumber);
            }

            if (iterNumber == 3) {
                // все, сделали все, что хотели
                executed = true;
                return null;
            } else {
                // сделали очередной шаг, хотим зафиксировать это состояние
                var resultState = JsonNodeFactory.instance.objectNode();
                resultState.put(LAST_ITER_NUM, iterNumber);
                return resultState;
            }
        }

        public void setInterruptOnIteration(int iterNumber) {
            interruptionIteration = iterNumber;
        }
    }

}
