package ru.yandex.travel.orders.services.finances.billing;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeEntityNode;
import ru.yandex.misc.io.RuntimeIoException;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.finances.BillingTransaction;
import ru.yandex.travel.orders.entities.finances.BillingTransactionKind;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionType;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BillingTransactionYtTableClientTest {
    private Yt yt;
    private BillingTransactionYtTableClient client;
    private YTreeEntityNode fakeSchema;

    @Before
    public void init() {
        BillingTransactionYtTableClientProperties properties = BillingTransactionYtTableClientProperties.builder()
                .tablesDirectory("//some/dir")
                .incomeTablesDirectory("//some/income/dir")
                .transactionDuration(Duration.ZERO)
                .build();
        yt = Mockito.mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        client = new BillingTransactionYtTableClient(properties, yt);
        fakeSchema = YTree.entityNode();
        fakeSchema.putAttribute("schema", YTree.listBuilder().buildList());
    }

    @Test
    public void exportTransactions() {
        YtTables tableApi = yt.tables();
        List<JsonNode> written = new ArrayList<>();
        doAnswer(call -> {
            IteratorF<JsonNode> records = call.getArgument(4);
            written.addAll(Lists.newArrayList(records));
            return null;
        }).when(tableApi).write(anyGuid(), anyBoolean(), any(), any(), any());

        client.exportTransactions(LocalDate.parse("2019-12-11"), List.of(tx(1), tx(2)));
        assertThat(written).hasSize(2);
        verify(tableApi, times(1))
                .write(anyGuid(), anyBoolean(), any(), any(), any());
    }

    @Test
    public void exportTransactions_someAlreadyExported() {
        Cypress cypressApi = yt.cypress();
        YtTables tableApi = yt.tables();
        List<BillingTransaction> transactions = List.of(tx(1), tx(2), tx(3));
        List<JsonNode> written = new ArrayList<>();
        when(cypressApi.exists(anyGuid(), anyBoolean(), any())).thenReturn(true);
        when(cypressApi.get(anyGuid(), anyBoolean(), any(), anyCollection())).thenReturn(YTree.integerNode(2L));
        when(cypressApi.get(anyGuid(), anyBoolean(), any(), eq(Cf.set("schema")))).thenReturn(fakeSchema);
        doAnswer(call -> {
            IteratorF<JsonNode> records = call.getArgument(4);
            written.addAll(Lists.newArrayList(records));
            return null;
        }).when(tableApi).write(anyGuid(), anyBoolean(), any(), any(), any());

        client.exportTransactions(LocalDate.parse("2019-12-11"), transactions);
        assertThat(written).hasSize(1).first().matches(txj -> txj.get("transaction_id").longValue() == 3);
        verify(tableApi, times(1)).write(anyGuid(), anyBoolean(), any(), any(), any());
        verify(cypressApi, times(1)).set(anyNullableGuid(), anyBoolean(), any(), eq(3L));
    }

    @Test
    public void exportTransactions_allAlreadyExported() {
        Cypress cypressApi = yt.cypress();
        YtTables tableApi = yt.tables();
        List<BillingTransaction> transactions = List.of(tx(1), tx(2), tx(3));
        when(cypressApi.exists(anyGuid(), anyBoolean(), any())).thenReturn(true);
        when(cypressApi.get(anyGuid(), anyBoolean(), any(), anyCollection())).thenReturn(YTree.integerNode(3L));
        when(cypressApi.get(anyGuid(), anyBoolean(), any(), eq(Cf.hashSet("schema")))).thenReturn(fakeSchema);

        client.exportTransactions(LocalDate.parse("2019-12-11"), transactions);
        verify(tableApi, times(0)).write(anyGuid(), anyBoolean(), any(), any(), any());
        verify(cypressApi, times(0)).set(anyNullableGuid(), anyBoolean(), any(), eq(3L));
    }

    @Test
    public void checkIds() {
        client.checkIds(List.of(tx(1), tx(2)));

        BillingTransaction txNoId = tx(0);
        txNoId.setYtId(null);
        assertThatThrownBy(() -> client.checkIds(List.of(txNoId)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction without YT id");

        BillingTransaction txNoOrigId = tx(0);
        txNoOrigId.getOriginalTransaction().setYtId(null);
        assertThatThrownBy(() -> client.checkIds(List.of(txNoOrigId)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Original transaction without YT id");

        assertThatThrownBy(() -> client.checkIds(List.of(tx(1), tx(1))))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction YT id: 1");
    }

    @Test
    public void checkTransactionsDate() {
        List<BillingTransaction> transactions = List.of(tx(1), tx(2));
        client.checkTransactionsDate(LocalDate.parse("2019-12-11"), transactions);

        transactions.get(0).setPayoutAt(Instant.parse("2019-12-12T00:00:00.01Z"));
        assertThatThrownBy(() -> client.checkTransactionsDate(LocalDate.parse("2019-12-11"), transactions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payout date mismatch")
                .hasMessageContaining("expected date: 2019-12-11, actual: 2019-12-12");
    }

    @Test
    public void getDestinationTablePath() {
        assertThat(client.getDestinationTablePath(LocalDate.parse("2019-12-11"), BillingTransactionKind.PAYMENT))
                .isEqualTo(YPath.simple("//some/dir/2019-12-11"));
        assertThat(client.getDestinationTablePath(LocalDate.parse("2019-12-11"), BillingTransactionKind.INCOME))
                .isEqualTo(YPath.simple("//some/income/dir/2019-12-11"));
    }

    @Test
    public void doInTx_success() {
        YtTransactions transactionApi = yt.transactions();
        client.doInTx(txId -> {});
        verify(transactionApi, times(1)).start(any());
        verify(transactionApi, times(1)).commit(any());
        verify(transactionApi, times(0)).abort(any());
    }

    @Test
    public void doInTx_failureOnAction() {
        YtTransactions transactionApi = yt.transactions();
        assertThatThrownBy(() -> client.doInTx(txId -> {
            throw new RuntimeIoException("Happy Y4eHbKu Day!");
        })).isExactlyInstanceOf(RuntimeIoException.class)
                .hasMessageContaining("Y4eHbKu");
        verify(transactionApi, times(1)).start(any());
        verify(transactionApi, times(0)).commit(any());
        verify(transactionApi, times(1)).abort(any());
    }

    @Test
    public void doInTx_failureOnCommit() {
        YtTransactions transactionApi = yt.transactions();
        doThrow(new RuntimeIoException("Happy Y4eHbKu Day!")).when(transactionApi).commit(any());
        assertThatThrownBy(() -> client.doInTx(txId -> {}))
                .isExactlyInstanceOf(RuntimeIoException.class)
                .hasMessageContaining("Y4eHbKu");
        verify(transactionApi, times(1)).start(any());
        verify(transactionApi, times(1)).commit(any());
        verify(transactionApi, times(1)).abort(any());
    }

    @Test
    public void doInTx_failureOnAbort() {
        YtTransactions transactionApi = yt.transactions();
        Exception oneMoreException = new RuntimeException("Not enough network exceptions");
        doThrow(oneMoreException).when(transactionApi).abort(any());
        assertThatThrownBy(() -> client.doInTx(txId -> {
            throw new RuntimeIoException("Happy Y4eHbKu Day!");
        })).isExactlyInstanceOf(RuntimeIoException.class)
                .hasMessageContaining("Y4eHbKu")
                .hasSuppressedException(oneMoreException);
        verify(transactionApi, times(1)).start(any());
        verify(transactionApi, times(0)).commit(any());
        verify(transactionApi, times(1)).abort(any());
    }

    @Test
    public void ensureTransactionsTableExists() {
        Cypress cypressApi = yt.cypress();
        client.ensureTransactionsTableExists(GUID.create(), YPath.simple("//test_path"), BillingTransactionKind.PAYMENT);
        verify(cypressApi, times(1)).exists(anyGuid(), anyBoolean(), any());
        verify(cypressApi, times(1)).create(any());
    }

    @Test
    public void ensureTransactionsTableExists_alreadyExists() {
        Cypress cypressApi = yt.cypress();
        when(cypressApi.exists(anyGuid(), anyBoolean(), any())).thenReturn(true);
        client.ensureTransactionsTableExists(GUID.create(), null, BillingTransactionKind.PAYMENT);
        verify(cypressApi, times(1)).exists(anyGuid(), anyBoolean(), any());
        verify(cypressApi, times(0)).create(any());
    }

    @Test
    public void getTransactionsTableColumns() {
        assertThat(client.getTransactionsTableColumns(BillingTransactionKind.PAYMENT)).isEqualTo(Set.of(
                "service_id", "transaction_id", "orig_transaction_id",
                "transaction_type", "payment_type", "paysys_type_cc",
                "partner_id", "price", "currency", "trust_payment_id",
                "client_id", "service_order_id", "dt", "update_dt"
        ));
        assertThat(client.getTransactionsTableColumns(BillingTransactionKind.INCOME)).isEqualTo(Set.of(
                "service_id", "transaction_id", "orig_transaction_id",
                "transaction_type",
                "amount", "currency",
                "client_id", "service_order_id", "dt"
        ));
    }

    @Test
    public void readTransactions() {
        YtTables tableApi = yt.tables();
        List<JsonNode> transactions = List.of(txJson(1), txJson(2));
        when(tableApi.<JsonNode>read(anyGuid(), anyBoolean(), any(), any()))
                .thenReturn(CloseableIterator.wrap(transactions));

        Map<Long, BillingTransaction> read = client.readTransactions(GUID.create(), null);
        Map<Long, BillingTransaction> expected = Map.of(1L, tx(1), 2L, tx(2));
        assertThat(read).isNotEqualTo(expected);
        for (BillingTransaction tx : expected.values()) {
            tx.setCreatedAt(tx.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
            tx.setPayoutAt(tx.getPayoutAt().truncatedTo(ChronoUnit.SECONDS));
            tx.setSourceFinancialEvent(null);
        }
        assertThat(read).isEqualTo(expected);
    }

    @Test
    public void readTransactions_changedSchema() {
        YtTables tableApi = yt.tables();
        ObjectNode tx = txJson(1);
        when(tableApi.<JsonNode>read(anyGuid(), anyBoolean(), any(), any()))
                .thenAnswer(inv -> CloseableIterator.wrap(List.<JsonNode>of(tx)));

        tx.remove("client_id");
        assertThatThrownBy(() -> client.readTransactions(GUID.create(), null, true))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected table schema");

        tx.put("client_id", 87635243L);
        assertThat(client.readTransactions(GUID.create(), null, true))
                .hasSize(1);

        tx.put("payout_update_ready_refresh_dt", "03.03");
        assertThatThrownBy(() -> client.readTransactions(GUID.create(), null, true))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected table schema");
    }

    @Test
    public void readTransactions_duplicateTransactions() {
        YtTables tableApi = yt.tables();
        List<JsonNode> transactions = List.of(txJson(1), txJson(1));
        when(tableApi.<JsonNode>read(anyGuid(), anyBoolean(), any(), any()))
                .thenReturn(CloseableIterator.wrap(transactions));

        assertThatThrownBy(() -> client.readTransactions(GUID.create(), null))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate transaction id");
    }

    @Test
    public void writeTransactions() {
        YtTables tableApi = yt.tables();
        BillingTransaction tx = tx(1);
        List<JsonNode> written = new ArrayList<>();
        doAnswer(call -> {
            IteratorF<JsonNode> records = call.getArgument(4);
            written.addAll(Lists.newArrayList(records));
            return null;
        }).when(tableApi).write(anyGuid(), anyBoolean(), any(), any(), any());

        client.writeTransactions(GUID.create(), YPath.simple("//some"), List.of(tx));
        assertThat(written).hasSize(1).first()
                // ensuring all enums and other data types and converted to basic types
                .extracting(txJson -> parseJson(serializeJson(txJson)))
                .isEqualTo(txJson(1));
    }

    @Test
    public void writeTransactions_empty() {
        client.writeTransactions(GUID.create(), null, List.of());
        verify(yt.tables(), times(0)).write(anyGuid(), anyBoolean(), any(), any(), any());
    }

    @Test
    public void writeTransactions_badPriceFormat() {
        BillingTransaction tx = tx(1);
        tx.setValue(Money.of(10000.001, "RUB"));
        assertThatThrownBy(() -> client.writeTransactions(GUID.create(), YPath.simple("//some"), List.of(tx)))
                .isExactlyInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Rounding necessary");
        verify(yt.tables(), times(0)).write(anyGuid(), anyBoolean(), any(), any(), any());
    }

    @Test
    public void filterTransactions() {
        assertThat(client.filterTransactions(List.of(tx(1), tx(2)), null, BillingTransactionKind.PAYMENT))
                .isEqualTo(List.of(tx(1), tx(2)));
        assertThat(client.filterTransactions(List.of(tx(1), tx(2)), 0L, BillingTransactionKind.PAYMENT))
                .isEqualTo(List.of(tx(1), tx(2)));
    }

    @Test
    public void filterTransactions_alreadyExported() {
        assertThat(client.filterTransactions(List.of(tx(1), tx(2)), 1L, BillingTransactionKind.PAYMENT))
                .isEqualTo(List.of(tx(2)));
    }

    private GUID anyNullableGuid() {
        return any();
    }

    private Optional<GUID> anyGuid() {
        return any();
    }

    private <T> Optional<T> anyOptional() {
        return any();
    }

    private BillingTransaction tx(long id) {
        HotelOrder fakeOrder = new HotelOrder();
        fakeOrder.setId(UUID.randomUUID());
        return BillingTransaction.builder()
                .serviceId(FinancialEventPaymentScheme.HOTELS.getServiceId())
                .ytId(id)
                .originalTransaction(BillingTransaction.builder().ytId(6251245124L).build())
                .sourceFinancialEvent(FinancialEvent.builder()
                        .order(fakeOrder)
                        .build())
                .transactionType(BillingTransactionType.REFUND)
                .paymentType(BillingTransactionPaymentType.COST)
                .paymentSystemType(BillingTransactionPaymentSystemType.YANDEX_MONEY)
                .partnerId(65412L)
                .serviceOrderId("YA-9834675")
                .createdAt(Instant.parse("2019-12-08T11:34:56.789012456Z"))
                .payoutAt(Instant.parse("2019-12-10T21:00:00.001Z"))
                .value(Money.of(1234.56, "RUB"))
                .trustPaymentId("12345678901234567890123")
                .clientId(87635243L)
                .kind(BillingTransactionKind.PAYMENT)
                .build();
    }

    private ObjectNode txJson(long id) {
        return parseJson("{\"service_id\":641,\"transaction_id\":" + id + "," +
                "\"transaction_type\":\"refund\",\"payment_type\":\"cost\",\"paysys_type_cc\":\"yamoney\"," +
                "\"partner_id\":65412,\"service_order_id\":\"YA-9834675\",\"dt\":\"2019-12-11T00:00:00\"," +
                "\"update_dt\":\"2019-12-08T14:34:56\",\"price\":\"1234.56\",\"currency\":\"RUB\"," +
                "\"trust_payment_id\":\"12345678901234567890123\",\"client_id\":87635243," +
                "\"orig_transaction_id\":6251245124}");
    }

    private ObjectNode parseJson(String content) {
        try {
            return (ObjectNode) new ObjectMapper().readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String serializeJson(JsonNode value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
