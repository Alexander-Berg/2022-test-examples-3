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
class CpaOrderTransferTaskTest extends FunctionalTest {

    @Autowired
    CpaOrderTransferTask cpaOrderTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(cpaOrderTransferTask.getName()).isEqualTo("cpa-order");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(cpaOrderTransferTask)).hasMessageContaining(
                "cpa-order");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "CpaOrderTransferTaskTest.commonData.before.csv",
            after = "CpaOrderTransferTaskTest.testCpaOrderTransferSuccess.after.csv"
    )
    void testCpaOrderFullTransferSuccess() {
        cpaOrderTransferTask.execute(createRangedTaskInput(101387921, 101388011), null);
    }

    @Test
    @DisplayName("Перенос всех данных с установленным размером batch_size")
    @DbUnitDataSet(
            before = "CpaOrderTransferTaskTest.commonData.before.csv",
            after = "CpaOrderTransferTaskTest.testCpaOrderTransferSuccess.after.csv"
    )
    void testCpaOrderBatchTransferSuccess() {
        ObjectNode taskInput = createRangedTaskInput(101387921, 101388011);
        taskInput.put("batch_size", Long.toString(2));

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, cpaOrderTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "CpaOrderTransferTaskTest.commonData.before.csv",
            after = "CpaOrderTransferTaskTest.testCpaOrderPartTransferSuccess.after.csv"
    )
    void testCpaOrderPartTransferSuccess() {
        cpaOrderTransferTask.execute(createRangedTaskInput(101387927, 101387981), null);
    }

    @Test
    @DisplayName("Запуск с дефолтными параметрами - нет переноса данных, order_id не попадает в диапазон")
    @DbUnitDataSet(
            before = "CpaOrderTransferTaskTest.commonData.before.csv",
            after = "CpaOrderTransferTaskTest.testCpaOrderNoTransfer.after.csv"
    )
    void testCpaOrderNoTransfer() {
        cpaOrderTransferTask.execute(JsonNodeFactory.instance.objectNode(), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "CpaOrderTransferTaskTest.commonData.before.csv",
                    "CpaOrderTransferTaskTest.testUpdateExistingCpaOrderDataOnTransfer.before.csv"
            },
            after = "CpaOrderTransferTaskTest.testUpdateExistingCpaOrderDataOnTransfer.after.csv"
    )
    void testUpdateExistingCpaOrderDataOnTransfer() {
        cpaOrderTransferTask.execute(createRangedTaskInput(101387928, 101387944), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        return taskInput;
    }
}
