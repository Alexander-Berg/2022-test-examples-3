package ru.yandex.market.billing.ora2pg.tasks;

import java.time.LocalDate;

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

public class CpaAuctionRemainBonusTransferTaskTest extends FunctionalTest {

    @Autowired
    CpaAuctionRemainBonusTransferTask cpaAuctionRemainBonusTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Test
    void springInitialization() {
        assertThat(cpaAuctionRemainBonusTransferTask.getName()).isEqualTo("cpa-auction-remain-bonus");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(cpaAuctionRemainBonusTransferTask))
                .hasMessageContaining("cpa-auction-remain-bonus");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "CpaAuctionRemainBonusTransferTask.full.before.csv",
            after = "CpaAuctionRemainBonusTransferTask.full.after.csv"
    )
    void testRemainBonusFullTransferSuccess() {
        cpaAuctionRemainBonusTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    @Test
    @DisplayName("Перенос части данных")
    @DbUnitDataSet(
            before = "CpaAuctionRemainBonusTransferTask.full.before.csv",
            after = "CpaAuctionRemainBonusTransferTask.limit.after.csv"
    )
    void testRemainBonusPartTransferSuccess() {
        cpaAuctionRemainBonusTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-02-15")), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "CpaAuctionRemainBonusTransferTask.full.before.csv",
                    "CpaAuctionRemainBonusTransferTask.update.before.csv"
            },
            after = "CpaAuctionRemainBonusTransferTask.update.after.csv"
    )
    void testUpdateExistingRemainBonusDataOnTransfer() {
        cpaAuctionRemainBonusTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    ObjectNode createTimeIntervalTaskInput(LocalDate from, LocalDate to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());
        return taskInput;
    }
}
