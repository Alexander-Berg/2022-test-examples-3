package ru.yandex.market.billing.ora2pg.tasks;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ParametersAreNonnullByDefault
public class OrderTrantimesTransferTaskTest extends FunctionalTest {

    @Autowired
    OrderTrantimesTransferTask orderTrantimesTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Test
    void springInitialization() {
        assertThat(orderTrantimesTransferTask.getName()).isEqualTo("order-trantimes");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(orderTrantimesTransferTask)).hasMessageContaining(
                "order-trantimes");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "OrderTrantimesTransferTaskTest.commonData.before.csv",
            after = "OrderTrantimesTransferTaskTest.fullTransfer.after.csv"
    )
    void fullTransfer() {
        orderTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-01", "2022-01-06"), null);
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "OrderTrantimesTransferTaskTest.commonData.before.csv",
            after = "OrderTrantimesTransferTaskTest.limit.after.csv"
    )
    void limit() {
        orderTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-02", "2022-01-05"), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "OrderTrantimesTransferTaskTest.commonData.before.csv",
                    "OrderTrantimesTransferTaskTest.updateExisting.before.csv"
            },
            after = "OrderTrantimesTransferTaskTest.fullTransfer.after.csv"
    )
    void updateExisting() {
        orderTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-01", "2022-01-06"), null);
    }

    ObjectNode createTimeIntervalTaskInput(String from, String to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from);
        taskInput.put("to", to);
        return taskInput;
    }
}
