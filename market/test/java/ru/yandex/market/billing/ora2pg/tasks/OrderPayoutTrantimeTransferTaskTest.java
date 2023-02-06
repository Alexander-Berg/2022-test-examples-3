package ru.yandex.market.billing.ora2pg.tasks;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
public class OrderPayoutTrantimeTransferTaskTest extends FunctionalTest {

    @Autowired
    OrderPayoutTrantimeTransferTask orderPayoutTrantimeTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(orderPayoutTrantimeTransferTask.getName()).isEqualTo("order-payout");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(orderPayoutTrantimeTransferTask))
                .hasMessageContaining("order-payout");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "OrderPayoutTrantimeTransferTaskTest.commonData.before.csv",
            after = "OrderPayoutTrantimeTransferTaskTest.fullTransfer.after.csv"
    )
    void fullTransfer() {
        orderPayoutTrantimeTransferTask.execute(createRangedTaskInput(110, 120), null);
    }

    @Test
    @DisplayName("Перенос всех данных в несколько итераций с ограничением id")
    @DbUnitDataSet(
            before = "OrderPayoutTrantimeTransferTaskTest.commonData.before.csv",
            after = "OrderPayoutTrantimeTransferTaskTest.iterationTransfer.after.csv"
    )
    void iterationTransfer() {
        ObjectNode taskInput = createRangedTaskInput(100, 114, 2);

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, orderPayoutTrantimeTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "OrderPayoutTrantimeTransferTaskTest.commonData.before.csv",
                    "OrderPayoutTrantimeTransferTaskTest.updateExisting.before.csv"
            },
            after = "OrderPayoutTrantimeTransferTaskTest.updateExisting.after.csv"
    )
    void updateExisting() {
        orderPayoutTrantimeTransferTask.execute(createRangedTaskInput(110, 120), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        return createRangedTaskInput(fromId, toId, 8);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId, long batchSize) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        taskInput.put("batch_size", Long.toString(batchSize));
        return taskInput;
    }
}
