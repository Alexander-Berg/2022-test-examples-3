package ru.yandex.market.billing.ora2pg.tasks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
class OrderTransactionTransferTaskTest extends FunctionalTest {

    @Autowired
    OrderTransactionTransferTask orderTransactionTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    JdbcTemplate pgJdbcTemplate;

    @Test
    void springInitialization() {
        assertThat(orderTransactionTransferTask.getName()).isEqualTo("order-transaction");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(orderTransactionTransferTask))
                .hasMessageContaining("order-transaction");
    }

    @Test
    @DisplayName("Один заказ - одна транзакция")
    @DbUnitDataSet(
            before = "OrderTransactionTransferTaskTest.oneToOne.before.csv",
            after = "OrderTransactionTransferTaskTest.oneToOne.after.csv")
    public void oneOrderToOneTransaction() {
        orderTransactionTransferTask.execute(input(110, 200), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L),
                444L, Set.of(112L)
        ));
    }

    @Test
    @DisplayName("Один заказ - несколько транзакций")
    @DbUnitDataSet(
            before = "OrderTransactionTransferTaskTest.oneToMany.before.csv",
            after = "OrderTransactionTransferTaskTest.oneToMany.after.csv")
    public void oneOrderToManyTransactions() {
        orderTransactionTransferTask.execute(input(110, 200), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L),
                444L, Set.of(111L),
                555L, Set.of(112L),
                666L, Set.of(112L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций")
    @DbUnitDataSet(
            before = "OrderTransactionTransferTaskTest.manyToMany.before.csv",
            after = "OrderTransactionTransferTaskTest.manyToMany.after.csv")
    public void manyOrdersToManyTransactions() {
        orderTransactionTransferTask.execute(input(110, 200, 10), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L, 113L, 114L, 116L),
                444L, Set.of(111L, 116L),
                555L, Set.of(112L),
                666L, Set.of(111L, 112L, 115L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций (в несколько итераций с повторной вставкой транзакций)")
    @DbUnitDataSet(
            before = "OrderTransactionTransferTaskTest.manyToMany.before.csv",
            after = "OrderTransactionTransferTaskTest.manyToMany.after.csv")
    public void manyOrdersToManyTransactionsInSeveralIterations() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "order-transaction",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(110, 200, 2))
        );
        transferOraToPgCommand.execute(new CommandInvocation("", new String[0], options), null);

        checkTablesConnection(Map.of(
                333L, Set.of(111L, 113L, 114L, 116L),
                444L, Set.of(111L, 116L),
                555L, Set.of(112L),
                666L, Set.of(111L, 112L, 115L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций (в несколько итераций c ограничением по order_id)")
    @DbUnitDataSet(
            before = "OrderTransactionTransferTaskTest.manyToMany.before.csv",
            after = "OrderTransactionTransferTaskTest.manyToManyLimited.after.csv")
    public void manyOrdersToManyTransactionsWithLimit() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "order-transaction",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(112, 115, 2))
        );
        transferOraToPgCommand.execute(new CommandInvocation("", new String[0], options), null);

        checkTablesConnection(Map.of(
                333L, Set.of(113L, 114L),
                555L, Set.of(112L),
                666L, Set.of(112L, 115L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций (все транзакции и связи с заказами уже существуют)")
    @DbUnitDataSet(
            before = {
                    "OrderTransactionTransferTaskTest.manyToMany.before.csv",
                    "OrderTransactionTransferTaskTest.manyToManyAllExisting.before.csv"
            },
            after = "OrderTransactionTransferTaskTest.manyToManyAllExisting.after.csv")
    public void manyOrdersToManyTransactionsWithAllExisting() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "order-transaction",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(110, 200, 3))
        );
        transferOraToPgCommand.execute(new CommandInvocation("", new String[0], options), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L, 113L, 114L, 116L),
                444L, Set.of(111L, 116L),
                555L, Set.of(112L),
                666L, Set.of(111L, 112L, 115L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций " +
            "(все транзакции существуют, но не у всех есть связь с заказами)")
    @DbUnitDataSet(
            before = {
                    "OrderTransactionTransferTaskTest.manyToMany.before.csv",
                    "OrderTransactionTransferTaskTest.manyToManyAllExistingWithoutConnection.before.csv"
            },
            after = "OrderTransactionTransferTaskTest.manyToManyAllExistingWithoutConnection.after.csv")
    public void manyOrdersToManyTransactionsWithAllExistingTransactionsButNotConnectedToAllOrders() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "order-transaction",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(110, 200, 2))
        );
        transferOraToPgCommand.execute(new CommandInvocation("", new String[0], options), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L, 113L, 114L, 116L),
                444L, Set.of(111L, 116L),
                555L, Set.of(112L),
                666L, Set.of(111L, 112L, 115L)
        ));
    }

    @Test
    @DisplayName("Несколько заказов - несколько транзакций " +
            "(не все транзакции существуют, и не у всех существующих есть связь с заказами)")
    @DbUnitDataSet(
            before = {
                    "OrderTransactionTransferTaskTest.manyToMany.before.csv",
                    "OrderTransactionTransferTaskTest.manyToManySeveralExistingWithoutConnection.before.csv"
            },
            after = "OrderTransactionTransferTaskTest.manyToManySeveralExistingWithoutConnection.after.csv")
    public void manyOrdersToManyTransactionsWithSeveralExistingTransactionsButNotConnectedToAllOrders()
            throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "order-transaction",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(110, 200, 2))
        );
        transferOraToPgCommand.execute(new CommandInvocation("", new String[0], options), null);
        checkTablesConnection(Map.of(
                333L, Set.of(111L, 113L, 114L, 116L),
                444L, Set.of(111L, 116L),
                555L, Set.of(112L),
                666L, Set.of(111L, 112L, 115L)
        ));
    }

    private void checkTablesConnection(Map<Long, Set<Long>> transactionIdToExpectedOrderIdsMap) {
        String select = "" +
                "select o.order_id as order_id, t.transaction_id as transaction_id " +
                "from market_billing.cpa_order_transaction as t " +
                "join market_billing.orders_transactions as o on o.transaction_id = t.id";
        List<Map<String, Object>> results = pgJdbcTemplate.queryForList(select);

        Map<Long, Set<Long>> userIdToActualOrderIdsMap = StreamEx.of(results)
                .mapToEntry(result -> (Long) result.get("transaction_id"), result -> (Long) result.get("order_id"))
                .grouping(Collectors.toSet());

        transactionIdToExpectedOrderIdsMap.forEach((transactionId, expectedOrderIds) -> {
            Set<Long> actualOrderIds = userIdToActualOrderIdsMap.get(transactionId);
            assertThat(actualOrderIds)
                    .describedAs("order_id, привязанные к транзакции transaction_id = %s", transactionId)
                    .isEqualTo(expectedOrderIds);
            userIdToActualOrderIdsMap.remove(transactionId);
        });
        assertThat(userIdToActualOrderIdsMap)
                .describedAs("не должно быть неожиданных записей")
                .isEmpty();
    }

    private JsonNode input(long fromOrderId, long toOrderId) {
        return input(fromOrderId, toOrderId, 5L);
    }

    private JsonNode input(long fromOrderId, long toOrderId, long batchSize) {
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        node.put("from_id", fromOrderId);
        node.put("to_id", toOrderId);
        node.put("batch_size", batchSize);
        return node;
    }
}
