package ru.yandex.market.billing.ora2pg.tasks;

import java.time.LocalDate;

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
public class BpoiTransferTaskTest extends FunctionalTest {

    @Autowired
    BpoiTransferTask bpoiTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(bpoiTransferTask.getName()).isEqualTo("bpoi");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(bpoiTransferTask)).hasMessageContaining(
                "bpoi");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "BpoiTransferTaskTest.commonData.before.csv",
            after = "BpoiTransferTaskTest.testTransferSuccess.after.csv"
    )
    void testCoshFullTransferSuccess() {
        bpoiTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "BpoiTransferTaskTest.commonData.before.csv",
                    "BpoiTransferTaskTest.testUpdateExistingDataOnTransfer.before.csv"
            },
            after = "BpoiTransferTaskTest.testUpdateExistingDataOnTransfer.after.csv"
    )
    void testUpdateExistingCpaOrderDataOnTransfer() {
        bpoiTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    ObjectNode createTimeIntervalTaskInput(LocalDate from, LocalDate to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());
        return taskInput;
    }
}
