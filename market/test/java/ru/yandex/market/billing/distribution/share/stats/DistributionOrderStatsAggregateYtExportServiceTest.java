package ru.yandex.market.billing.distribution.share.stats;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.billing.distribution.share.stats.DistributionOrderStatsDao;
import ru.yandex.market.core.stepevent.StepEventRestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class DistributionOrderStatsAggregateYtExportServiceTest extends FunctionalTest {

    private static final String CREATED_TABLE_PATH = "//some/yt/approved/path";
    private static final String APPROVED_TABLE_PATH = "//some/yt/created/path";
    private static final String FULL_CREATED_TABLE_PATH = "//some/yt/approved/path/2021-03-03";
    private static final String FULL_APPROVED_TABLE_PATH = "//some/yt/created/path/2021-03-03";

    private static final LocalDate LOCAL_DATE = LocalDate.of(2021, 3, 3);
    private static final Map<String, String> APPROVED_SCHEMA = ImmutableMap.<String, String>builder()
            .put("clid", "int64")
            .put("vid", "string")
            .put("promocode", "string")
            .put("orders_count", "int64")
            .put("partner_payment_no_vat", "double")
            .put("orders_billed_cost", "double")
            .put("date", "string")
            .build();

    private static final Map<String, String> CREATED_SCHEMA = ImmutableMap.<String, String>builder()
            .put("clid", "int64")
            .put("vid", "string")
            .put("promocode", "string")
            .put("orders_count", "int64")
            .put("orders_count_cancelled", "int64")
            .put("orders_count_not_cancelled", "int64")
            .put("partner_payment_no_vat_max", "double")
            .put("partner_payment_no_vat", "double")
            .put("orders_billed_cost", "double")
            .put("orders_billed_cost_ignoring_cancelled", "double")
            .put("date", "string")
            .build();

    @Mock
    private YtTransactions ytTransactionsHahn;
    @Mock
    private Transaction transactionHahn;
    @Mock
    private YtTables ytTablesMockHahn;
    @Mock
    private Cypress cypressMockHahn;

    @Mock
    private YtTransactions ytTransactionsArnold;
    @Mock
    private Transaction transactionArnold;
    @Mock
    private YtTables ytTablesMockArnold;
    @Mock
    private Cypress cypressMockArnold;

    @Mock
    private Yt ytMockHahn;

    @Mock
    private Yt ytMockArnold;

    @Mock
    private StepEventRestClient stepEventRestClient;

    @Autowired
    private DistributionOrderStatsDao distributionOrderStatsDao;

    private DistributionOrderStatsAggregateYtExportService distributionOrderStatsAggregateYtExportService;

    @BeforeEach
    void setup() {
        setupYtMock();
        this.distributionOrderStatsAggregateYtExportService = new DistributionOrderStatsAggregateYtExportService(
                ytMockHahn,
                ytMockArnold,
                CREATED_TABLE_PATH,
                APPROVED_TABLE_PATH,
                distributionOrderStatsDao,
                stepEventRestClient
        );
    }

    private void setupYtMock() {
        final var transactionIdHahn = Mockito.mock(GUID.class);
        Mockito.when(ytTransactionsHahn.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transactionHahn);
        Mockito.when(transactionHahn.getId())
                .thenReturn(transactionIdHahn);

        Mockito.when(ytMockHahn.tables()).thenReturn(ytTablesMockHahn);
        Mockito.when(ytMockHahn.cypress()).thenReturn(cypressMockHahn);
        Mockito.when(ytMockHahn.transactions()).thenReturn(ytTransactionsHahn);


        final var transactionIdArnold = Mockito.mock(GUID.class);
        Mockito.when(ytTransactionsArnold.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transactionArnold);
        Mockito.when(transactionArnold.getId())
                .thenReturn(transactionIdArnold);

        Mockito.when(ytMockArnold.tables()).thenReturn(ytTablesMockArnold);
        Mockito.when(ytMockArnold.cypress()).thenReturn(cypressMockArnold);
        Mockito.when(ytMockArnold.transactions()).thenReturn(ytTransactionsArnold);
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsAggregateYtExportServiceTest.testExport.before.csv")
    void testExport() {
        distributionOrderStatsAggregateYtExportService.exportApprovedAggregate(LOCAL_DATE);
        distributionOrderStatsAggregateYtExportService.exportCreatedAggregate(LOCAL_DATE);

        final var schemaCaptor = ArgumentCaptor.forClass(MapF.class);

        verifyCreate(schemaCaptor, FULL_APPROVED_TABLE_PATH, cypressMockHahn);
        verifyCreate(schemaCaptor, FULL_CREATED_TABLE_PATH, cypressMockHahn);
        verifyCreate(schemaCaptor, FULL_APPROVED_TABLE_PATH, cypressMockArnold);
        verifyCreate(schemaCaptor, FULL_CREATED_TABLE_PATH, cypressMockArnold);

        Mockito.verifyNoMoreInteractions(cypressMockHahn);
        Mockito.verifyNoMoreInteractions(cypressMockArnold);

        verifySchema(schemaCaptor.getAllValues());

        final var entriesCaptor = ArgumentCaptor.forClass(Iterator.class);


        verifyWrite(entriesCaptor, FULL_APPROVED_TABLE_PATH, ytTablesMockArnold);
        verifyWrite(entriesCaptor, FULL_CREATED_TABLE_PATH, ytTablesMockHahn);
        verifyWrite(entriesCaptor, FULL_APPROVED_TABLE_PATH, ytTablesMockHahn);
        verifyWrite(entriesCaptor, FULL_CREATED_TABLE_PATH, ytTablesMockArnold);

        Mockito.verifyNoMoreInteractions(ytTablesMockHahn);
        Mockito.verifyNoMoreInteractions(ytTablesMockArnold);

        final var exportedEntities = entriesCaptor.getAllValues();

        assertThat(exportedEntities).hasSize(4);

        final var exportedEntity1 = ImmutableList.copyOf(exportedEntities.get(0));
        final var exportedEntity2 = ImmutableList.copyOf(exportedEntities.get(1));
        final var exportedEntity3 = ImmutableList.copyOf(exportedEntities.get(2));
        final var exportedEntity4 = ImmutableList.copyOf(exportedEntities.get(3));

        assertEquals(1, exportedEntity1.size());
        assertEquals(3, exportedEntity2.size());
        assertEquals(1, exportedEntity3.size());
        assertEquals(3, exportedEntity4.size());

        String created = StringTestUtil.getString(getClass(), "json/DistributionOrderStatsAggregateYtExportServiceTest" +
                ".testExport.created.json");
        String approved = StringTestUtil.getString(getClass(), "json/DistributionOrderStatsAggregateYtExportServiceTest" +
                ".testExport.approved.json");

        JSONAssert.assertEquals(
                created,
                exportedEntity1.toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
        JSONAssert.assertEquals(
                approved,
                exportedEntity2.toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
        JSONAssert.assertEquals(
                created,
                exportedEntity3.toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
        JSONAssert.assertEquals(
                approved,
                exportedEntity4.toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
        Mockito.verifyNoMoreInteractions(ytTablesMockHahn);
        Mockito.verifyNoMoreInteractions(ytTablesMockArnold);
    }

    private void verifyWrite(ArgumentCaptor<Iterator> entitiesCaptor, String tablePath, YtTables ytTablesMock) {
        Mockito.verify(ytTablesMock)
                .write(
                        any(), anyBoolean(),
                        eq(YPath.simple(tablePath)),
                        eq(YTableEntryTypes.JACKSON_UTF8),
                        entitiesCaptor.capture()
                );
    }

    private void verifyCreate(
            ArgumentCaptor<MapF> schemaCaptor,
            String tablePath,
            Cypress cypressMock) {
        Mockito.verify(cypressMock)
                .create(
                        any(), anyBoolean(),
                        eq(YPath.simple(tablePath)),
                        eq(CypressNodeType.TABLE),
                        eq(true), eq(false), eq(true),
                        schemaCaptor.capture()
                );
    }

    private void verifySchema(List<MapF> actualSchema) {
        assertEquals(4, actualSchema.size()); // created, approved; hahn, arnold
        verifySchema(APPROVED_SCHEMA, (YTreeListNode) actualSchema.get(3).getOrThrow("schema"));
        verifySchema(CREATED_SCHEMA, (YTreeListNode) actualSchema.get(2).getOrThrow("schema"));
    }

    private void verifySchema(Map<String, String> expectedSchema, YTreeListNode actualSchema) {
        assertEquals(expectedSchema.size(), actualSchema.size());
        for (YTreeNode field : actualSchema) {
            final var fieldName = field.mapNode().getString("name");
            final var fieldType = field.mapNode().getString("type");
            assertEquals(expectedSchema.get(fieldName), fieldType, "mismatch on " + fieldName);
        }
    }
}
