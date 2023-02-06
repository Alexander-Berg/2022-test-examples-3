package ru.yandex.market.billing.report.fulfillment.supply;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.report.fulfillment.supply.exception.YtExportException;
import ru.yandex.market.billing.report.fulfillment.supply.exception.YtReportDataExportException;
import ru.yandex.market.billing.util.yt.YtFactory;
import ru.yandex.market.billing.util.yt.YtFactoryImpl;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.util.MbiAsserts.assertJsonEquals;

class StocksBySupplyReportYTExportServiceTest extends FunctionalTest {

    private static final Map<String, String> EXPECTED_SCHEMA = ImmutableMap.<String, String>builder()
            .put("supplier_id", "int64")
            .put("supply_id", "int64")
            .put("shop_sku", "string")
            .put("billing_timestamp", "string")
            .put("start_timestamp", "string")
            .put("items_in_supply", "int64")
            .put("items_on_stock", "int64")
            .put("operation_type", "string")
            .put("type", "string")
            .put("days_on_stock", "int64")
            .put("days_to_pay", "int64")
            .put("days_of_paid_storage", "int64")
            .put("weight", "string")
            .put("length", "int64")
            .put("width", "int64")
            .put("height", "int64")
            .put("tariff", "string")
            .put("total_amount", "int64")
            .build();

    @Autowired
    @Value("${market.billing.reports.stocks-by-supply.export.yt.path}")
    private String ytTablePath;

    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> ytWritenEntitiesCaptor;
    @Mock
    private Yt ytMock;
    @Mock
    private YtTransactions ytTransactions;
    @Mock
    private Transaction transaction;
    @Mock
    private YtTables ytTablesMock;
    @Mock
    private Cypress cypressMock;
    @Mock
    private GUID transactionId;

    @Autowired
    private StocksBySupplyReportDao dao;
    private static final LocalDate EXPORT_DATE = LocalDate.of(2021, 7, 14);

    @BeforeEach
    void setup() {
        when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);
        when(transaction.getId())
                .thenReturn(transactionId);
        setUpMock(ytMock);
    }

    private void setUpMock(Yt yt) {
        when(yt.tables()).thenReturn(ytTablesMock);
        when(yt.cypress()).thenReturn(cypressMock);
        when(yt.transactions()).thenReturn(ytTransactions);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        return mapper;
    }

    @Test
    @DbUnitDataSet(before = "db/StocksBySupplyReportYTExportServiceTest.testExport.csv")
    void testExport() throws YtExportException {
        StocksBySupplyReportYTExportService ytExportService =
                new StocksBySupplyReportYTExportService(
                        ignored -> ytMock,
                        List.of("cluster"),
                        createObjectMapper(),
                        ytTablePath
                );

        ytExportService.export(dao.getBilledEntitiesByDate(EXPORT_DATE), EXPORT_DATE);

        verify(ytTablesMock)
                .write(
                        any(),
                        eq(true),
                        argThat(yPath -> yPath.toString().equals(ytTablePath + "/2021-07-14")),
                        eq(YTableEntryTypes.JACKSON_UTF8),
                        ytWritenEntitiesCaptor.capture()
                );

        ArgumentCaptor<CreateNode> createNodeArgumentCaptor = ArgumentCaptor.forClass(CreateNode.class);
        verify(cypressMock)
                .create(createNodeArgumentCaptor.capture());
        verifySchema(createNodeArgumentCaptor.getValue());

        verifyNoMoreInteractions(ytTablesMock);

        List<JsonNode> actual = ImmutableList.copyOf(ytWritenEntitiesCaptor.getValue());

        String expectedJson = StringTestUtil.getString(
                getClass(),
                "json/StocksBySupplyReportDataDailyExportExecutorTest.testExport.json"
        );

        assertJsonEquals(expectedJson, actual.toString());
    }

    @Test
    void testExportIntoSeveralClusters() throws YtExportException {
        Yt otherYtMock = Mockito.mock(Yt.class);
        setUpMock(otherYtMock);

        Map<String, Yt> ytClusters = Map.of("cluster", ytMock, "other-cluster", otherYtMock);
        YtFactory ytFactory = new YtFactoryImpl(
                ytClusters
        );

        StocksBySupplyReportYTExportService ytExportService =
                new StocksBySupplyReportYTExportService(
                        ytFactory,
                        new ArrayList<>(ytClusters.keySet()),
                        createObjectMapper(),
                        ytTablePath
                );

        ytExportService.export(List.of(), EXPORT_DATE);

        Mockito.verify(ytMock).transactions();
        Mockito.verify(otherYtMock).transactions();
    }

    @Test
    void testYtClientExceptionOnExport() {
        Yt otherYtMock = Mockito.mock(Yt.class);
        Mockito.doThrow(RuntimeException.class)
                .when(otherYtMock)
                .transactions();

        Map<String, Yt> ytClusters = Map.of("cluster", ytMock, "other-cluster", otherYtMock);
        YtFactory ytFactory = new YtFactoryImpl(
                ytClusters
        );

        StocksBySupplyReportYTExportService ytExportService =
                new StocksBySupplyReportYTExportService(
                        ytFactory,
                        List.of("other-cluster", "cluster"),
                        createObjectMapper(),
                        ytTablePath
                );

        assertThatCode(() -> ytExportService.export(List.of(), EXPORT_DATE))
                .isExactlyInstanceOf(YtExportException.class)
                .hasMessage("Some clusters raise exception on export");

        Mockito.verify(ytMock)
                .cypress();
    }

    @Test
    void testAllYtClientExceptionOnExport() {
        Yt otherYtMock = Mockito.mock(Yt.class);
        Mockito.doThrow(RuntimeException.class)
                .when(otherYtMock)
                .transactions();

        Mockito.doThrow(RuntimeException.class)
                .when(ytMock)
                .transactions();

        Map<String, Yt> ytClusters = Map.of("cluster", ytMock, "other-cluster", otherYtMock);
        YtFactory ytFactory = new YtFactoryImpl(
                ytClusters
        );

        StocksBySupplyReportYTExportService ytExportService =
                new StocksBySupplyReportYTExportService(
                        ytFactory,
                        List.of("other-cluster", "cluster"),
                        createObjectMapper(),
                        ytTablePath
                );

        assertThatCode(() -> ytExportService.export(List.of(), EXPORT_DATE))
                .isExactlyInstanceOf(YtReportDataExportException.class)
                .hasMessage("All clusters raise exception on export");
    }

    private void verifySchema(CreateNode node) {
        List<YTreeNode> actualSchema = node.getAttributes()
                .get("schema")
                .asList();

        assertThat(actualSchema)
                .hasSize(EXPECTED_SCHEMA.size())
                .allMatch(schemaNode -> {
                    String field = schemaNode.mapNode().get("name").orElseThrow().stringValue();
                    String type = schemaNode.mapNode().get("type").orElseThrow().stringValue();
                    assertThat(EXPECTED_SCHEMA)
                            .containsEntry(field, type);

                    if (field.equals("supplier_id") || field.equals("shop_sku")) {
                        String sortOrder = schemaNode.mapNode()
                                .get("sort_order")
                                .orElseThrow()
                                .stringValue();

                        assertThat(sortOrder)
                                .isEqualTo("ascending");
                    }

                    return true;
                });
    }
}
