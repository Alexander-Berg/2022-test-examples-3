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
class CpaOrderItemTransferTaskTest extends FunctionalTest {

    @Autowired
    CpaOrderItemTransferTask cpaOrderItemTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(cpaOrderItemTransferTask.getName()).isEqualTo("cpa-order-item");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(cpaOrderItemTransferTask)).hasMessageContaining(
                "cpa-order-item");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "CpaOrderItemTransferTaskTest.commonData.before.csv",
            after = "CpaOrderItemTransferTaskTest.testCpaOrderItemFullTransferSuccess.after.csv"
    )
    void testCpaOrderItemFullTransferSuccess() {
        cpaOrderItemTransferTask.execute(createRangedTaskInput(166729780, 166729820), null);
    }

    @Test
    @DisplayName("Перенос всех данных с установленным размером batch_size")
    @DbUnitDataSet(
            before = "CpaOrderItemTransferTaskTest.commonData.before.csv",
            after = "CpaOrderItemTransferTaskTest.testCpaOrderItemFullTransferSuccess.after.csv"
    )
    void testCpaOrderItemBatchTransferSuccess() {
        ObjectNode taskInput = createRangedTaskInput(166729780, 166729820);
        taskInput.put("batch_size", Long.toString(2));

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, cpaOrderItemTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "CpaOrderItemTransferTaskTest.commonData.before.csv",
            after = "CpaOrderItemTransferTaskTest.testCpaOrderItemPartTransferSuccess.after.csv"
    )
    void testCpaOrderItemPartTransferSuccess() {
        cpaOrderItemTransferTask.execute(createRangedTaskInput(166729796, 166729813), null);
    }

    @Test
    @DisplayName("Запуск с дефолтными параметрами - нет переноса данных, id не попадает в диапазон")
    @DbUnitDataSet(
            before = "CpaOrderItemTransferTaskTest.commonData.before.csv",
            after = "CpaOrderItemTransferTaskTest.testCpaOrderItemNoTransfer.after.csv"
    )
    void testCpaOrderItemNoTransfer() {
        cpaOrderItemTransferTask.execute(JsonNodeFactory.instance.objectNode(), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "CpaOrderItemTransferTaskTest.commonData.before.csv",
                    "CpaOrderItemTransferTaskTest.testUpdateExistingCpaOrderItemDataOnTransfer.before.csv"
            },
            after = "CpaOrderItemTransferTaskTest.testUpdateExistingCpaOrderItemDataOnTransfer.after.csv"
    )
    void testUpdateExistingCpaOrderItemDataOnTransfer() {
        cpaOrderItemTransferTask.execute(createRangedTaskInput(166729810, 166729812), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        return taskInput;
    }
}
