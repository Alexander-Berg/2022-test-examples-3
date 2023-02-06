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
public class CpaAuctionTrantimesTransferTaskTest extends FunctionalTest {

    @Autowired
    CpaAuctionTrantimesTransferTask cpaAuctionTrantimesTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Test
    void springInitialization() {
        assertThat(cpaAuctionTrantimesTransferTask.getName()).isEqualTo("cpa-auction-trantimes");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(cpaAuctionTrantimesTransferTask))
                .hasMessageContaining("cpa-auction-trantimes");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "CpaAuctionTrantimesTransferTaskTest.commonData.before.csv",
            after = "CpaAuctionTrantimesTransferTaskTest.fullTransfer.after.csv"
    )
    void fullTransfer() {
        cpaAuctionTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-01", "2022-01-06"), null);
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "CpaAuctionTrantimesTransferTaskTest.commonData.before.csv",
            after = "CpaAuctionTrantimesTransferTaskTest.limit.after.csv"
    )
    void limit() {
        cpaAuctionTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-02", "2022-01-05"), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "CpaAuctionTrantimesTransferTaskTest.commonData.before.csv",
                    "CpaAuctionTrantimesTransferTaskTest.updateExisting.before.csv"
            },
            after = "CpaAuctionTrantimesTransferTaskTest.fullTransfer.after.csv"
    )
    void updateExisting() {
        cpaAuctionTrantimesTransferTask.execute(
                createTimeIntervalTaskInput("2022-01-01", "2022-01-06"), null);
    }

    ObjectNode createTimeIntervalTaskInput(String from, String to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from);
        taskInput.put("to", to);
        return taskInput;
    }
}
