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
public class PromoOrderItemTransferTaskTest extends FunctionalTest {

    @Autowired
    PromoOrderItemTransferTask promoOrderItemTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    void testSpringInitialization() {
        assertThat(promoOrderItemTransferTask.getName()).isEqualTo("promo-order-item");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(promoOrderItemTransferTask)).hasMessageContaining(
                "promo-order-item");
    }

    @Test
    @DisplayName("Перенос всех данных")
    @DbUnitDataSet(
            before = "PromoOrderItemTransferTaskTest.commonData.before.csv",
            after = "PromoOrderItemTransferTaskTest.testTransferSuccess.after.csv"
    )
    void testCoshFullTransferSuccess() {
        promoOrderItemTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    @Test
    @DisplayName("Обновление существующих данных при переносе")
    @DbUnitDataSet(
            before = {
                    "PromoOrderItemTransferTaskTest.commonData.before.csv",
                    "PromoOrderItemTransferTaskTest.testUpdateExistingDataOnTransfer.before.csv"
            },
            after = "PromoOrderItemTransferTaskTest.testUpdateExistingDataOnTransfer.after.csv"
    )
    void testUpdateExistingCpaOrderDataOnTransfer() {
        promoOrderItemTransferTask.execute(
                createTimeIntervalTaskInput(LocalDate.parse("2022-02-01"), LocalDate.parse("2022-03-01")), null);
    }

    ObjectNode createTimeIntervalTaskInput(LocalDate from, LocalDate to) {
        var taskInput = JsonNodeFactory.instance.objectNode();
        taskInput.put("from", from.toString());
        taskInput.put("to", to.toString());
        return taskInput;
    }
}
