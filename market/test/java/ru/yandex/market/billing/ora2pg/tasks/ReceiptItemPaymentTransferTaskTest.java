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
class ReceiptItemPaymentTransferTaskTest extends FunctionalTest {

    @Autowired
    ReceiptItemPaymentTransferTask receiptItemPaymentTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(receiptItemPaymentTransferTask.getName()).isEqualTo("receipt-item-payment");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(receiptItemPaymentTransferTask))
                .hasMessageContaining("receipt-item-payment");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "ReceiptItemPaymentTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemPaymentTransferTaskTest.testReceiptItemPaymentTransferSuccess.after.csv"
    )
    void testReceiptItemPaymentTransferSuccess() {
        receiptItemPaymentTransferTask.execute(createRangedTaskInput(10_000_001, 10_000_010), null);
    }

    @Test
    @DisplayName("Запуск с дефолтными параметрами - перенос всех данных")
    @DbUnitDataSet(
            before = "ReceiptItemPaymentTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemPaymentTransferTaskTest.testReceiptItemPaymentTransferSuccess.after.csv"
    )
    void testReceiptItemPaymentDefaultParamsTransfer() {
        var taskInput = JsonNodeFactory.instance.objectNode();
        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, receiptItemPaymentTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );

    }

    @Test
    @DisplayName("Перенос всех данных с установленным размером batch_size")
    @DbUnitDataSet(
            before = "ReceiptItemPaymentTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemPaymentTransferTaskTest.testReceiptItemPaymentTransferSuccess.after.csv"
    )
    void testReceiptItemPaymentBatchTransferSuccess() {
        ObjectNode taskInput = createRangedTaskInput(10_000_001, 10_000_010);
        taskInput.put("batch_size", Long.toString(2));

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, receiptItemPaymentTransferTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "ReceiptItemPaymentTransferTaskTest.commonData.before.csv",
            after = "ReceiptItemPaymentTransferTaskTest.testReceiptItemPaymentPartTransferSuccess.after.csv"
    )
    void testReceiptItemPaymentPartTransferSuccess() {
        receiptItemPaymentTransferTask.execute(createRangedTaskInput(10000002, 10000006), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "ReceiptItemPaymentTransferTaskTest.commonData.before.csv",
                    "ReceiptItemPaymentTransferTaskTest.testUpdateExistingReceiptItemPaymentDataOnTransfer.before.csv"
            },
            after = "ReceiptItemPaymentTransferTaskTest.testReceiptItemPaymentTransferSuccess.after.csv"
    )
    void testUpdateExistingReceiptItemPaymentDataOnTransfer() {
        receiptItemPaymentTransferTask.execute(createRangedTaskInput(10_000_001, 10_000_010), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        return taskInput;
    }
}
