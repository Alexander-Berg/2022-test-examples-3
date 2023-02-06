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
class ReceiptItemRefundTransferTaskTest extends FunctionalTest {

    @Autowired
    ReceiptItemRefundTransferTask receiptItemRefundTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(receiptItemRefundTransferTask.getName()).isEqualTo("receipt-item-refund");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(receiptItemRefundTransferTask))
                .hasMessageContaining("receipt-item-refund");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "ReceiptItemRefundTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemRefundTransferTaskTest.testReceiptItemRefundTransferSuccess.after.csv"
    )
    void testReceiptItemRefundTransferSuccess() {
        receiptItemRefundTransferTask.execute(createRangedTaskInput(9_000_000, 9_000_020), null);
    }

    @Test
    @DisplayName("Запуск с дефолтными параметрами - перенос всех данных")
    @DbUnitDataSet(
            before = "ReceiptItemRefundTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemRefundTransferTaskTest.testReceiptItemRefundTransferSuccess.after.csv"
    )
    void testReceiptItemRefundDefaultParamsTransfer() {
        var taskInput = JsonNodeFactory.instance.objectNode();
        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, receiptItemRefundTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );

    }

    @Test
    @DisplayName("Перенос всех данных с установленным размером batch_size")
    @DbUnitDataSet(
            before = "ReceiptItemRefundTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemRefundTransferTaskTest.testReceiptItemRefundTransferSuccess.after.csv"
    )
    void testReceiptItemRefundBatchTransferSuccess() {
        ObjectNode taskInput = createRangedTaskInput(9_000_000, 9_000_020);
        taskInput.put("batch_size", Long.toString(2));

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, receiptItemRefundTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "ReceiptItemRefundTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemRefundTransferTaskTest.testReceiptItemRefundPartTransferSuccess.after.csv"
    )
    void testReceiptItemRefundPartTransferSuccess() {
        receiptItemRefundTransferTask.execute(createRangedTaskInput(9000004, 9000010), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "ReceiptItemRefundTransferTaskTest.commonData.before.csv",
                    "ReceiptItemRefundTransferTaskTest.testUpdateExistingReceiptItemRefundDataOnTransfer.before.csv"
            },
            after = "ReceiptItemRefundTransferTaskTest.testReceiptItemRefundTransferSuccess.after.csv"
    )
    void testUpdateExistingReceiptItemRefundDataOnTransfer() {
        receiptItemRefundTransferTask.execute(createRangedTaskInput(9_000_000, 9_000_020), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        return taskInput;
    }
}
