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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
class AccrualToAccrualNewCopyTaskTest extends FunctionalTest {

    @Autowired
    AccrualToAccrualNewCopyTask accrualToAccrualNewCopyTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(accrualToAccrualNewCopyTask.getName()).isEqualTo("accrual-new");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(accrualToAccrualNewCopyTask))
                .hasMessageContaining("accrual-new");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "AccrualToAccrualNewCopyTaskTest.commonData.before.csv",
            after = "AccrualToAccrualNewCopyTaskTest.testAccrualCopySuccess.after.csv"
    )
    void testAccrualCopySuccess() {
        accrualToAccrualNewCopyTask.execute(createRangedTaskInput(1, 11), null);
    }

    @Test
    @DisplayName("Перенос всех данных с установленным размером batch_size")
    @DbUnitDataSet(
            before = "AccrualToAccrualNewCopyTaskTest.commonData.before.csv",
            after = "AccrualToAccrualNewCopyTaskTest.testAccrualBatchCopySuccess.after.csv"
    )
    void testAccrualBatchCopySuccess() {
        ObjectNode taskInput = createRangedTaskInput(1, 11);
        taskInput.put("batch_size", Long.toString(2));

        transferOraToPgCommand.execute(
                new CommandInvocation(
                        TransferOraToPgCommand.TRANSFER_TO_PG,
                        new String[]{},
                        Map.ofEntries(
                                entry(TASK_NAME, accrualToAccrualNewCopyTask.getName()),
                                entry(TASK_INPUT, taskInput.toString())
                        )
                ),
                null
        );
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "AccrualToAccrualNewCopyTaskTest.commonData.before.csv",
            after = "AccrualToAccrualNewCopyTaskTest.testAccrualPartCopySuccess.after.csv"
    )
    void testAccrualPartCopySuccess() {
        accrualToAccrualNewCopyTask.execute(createRangedTaskInput(2, 6), null);
    }

    @Test
    @DisplayName("Падение при попытке обновить существующие данные")
    @DbUnitDataSet(
            before = {
                    "AccrualToAccrualNewCopyTaskTest.commonData.before.csv",
                    "AccrualToAccrualNewCopyTaskTest.accrualNew.before.csv"
            }
    )
    void testUpdateAccrualNewFail() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            accrualToAccrualNewCopyTask.execute(createRangedTaskInput(1, 11), null);
        });
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("ERROR: duplicate key value violates unique constraint"));
    }

    ObjectNode createRangedTaskInput(long fromId, long toId) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from_id", Long.toString(fromId));
        taskInput.put("to_id", Long.toString(toId));
        return taskInput;
    }
}
