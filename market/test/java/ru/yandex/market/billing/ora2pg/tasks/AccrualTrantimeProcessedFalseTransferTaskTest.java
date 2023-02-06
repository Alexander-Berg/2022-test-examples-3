package ru.yandex.market.billing.ora2pg.tasks;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ParametersAreNonnullByDefault
class AccrualTrantimeProcessedFalseTransferTaskTest extends FunctionalTest {

    @Autowired
    AccrualTrantimeProcessedFalseTransferTask accrualTrantimeTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(accrualTrantimeTransferTask.getName()).isEqualTo("accrual-processed-false");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(accrualTrantimeTransferTask)).hasMessageContaining(
                "accrual");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "AccrualTrantimeProcessedFalseTransferTaskTest.commonData.before.csv",
            after = "AccrualTrantimeProcessedFalseTransferTaskTest.fullTransfer.after.csv"
    )
    void fullTransfer() {
        accrualTrantimeTransferTask.execute(createRangedTaskInput(330, 340), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "AccrualTrantimeProcessedFalseTransferTaskTest.commonData.before.csv",
                    "AccrualTrantimeProcessedFalseTransferTaskTest.updateExisting.before.csv"
            },
            after = "AccrualTrantimeProcessedFalseTransferTaskTest.updateExisting.after.csv"
    )
    void updateExisting() {
        accrualTrantimeTransferTask.execute(createRangedTaskInput(300, 340), null);
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        taskInput.put("batch_size", Long.toString(8));
        return taskInput;
    }
}
