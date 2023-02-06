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
public class CoshTransferTaskTest extends FunctionalTest {

    @Autowired
    CoshTransferTask coshTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(coshTransferTask.getName()).isEqualTo("cpa-order-status-history");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(coshTransferTask)).hasMessageContaining(
                "cpa-order-status-history");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "CoshTransferTaskTest.commonData.before.csv",
            after = "CoshTransferTaskTest.testCoshTransferSuccess.after.csv"
    )
    void testCoshFullTransferSuccess() {
        coshTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "CoshTransferTaskTest.commonData.before.csv",
                    "CoshTransferTaskTest.testUpdateExistingCoshDataOnTransfer.before.csv"
            },
            after = "CoshTransferTaskTest.testUpdateExistingCoshDataOnTransfer.after.csv"
    )
    void testUpdateExistingCpaOrderDataOnTransfer() {
        coshTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    ObjectNode createTimeIntervalTaskInput(LocalDate from, LocalDate to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());
        return taskInput;
    }
}
