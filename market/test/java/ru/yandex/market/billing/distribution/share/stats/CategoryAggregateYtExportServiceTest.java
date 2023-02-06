package ru.yandex.market.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsDimensionAggregate;
import ru.yandex.market.core.stepevent.StepEventRestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mbi.util.MbiAsserts.assertJsonEquals;

@ExtendWith(MockitoExtension.class)
class CategoryAggregateYtExportServiceTest extends FunctionalTest {

    private static final String TABLE_PATH = "//some/yt/categories/path";
    private static final String FULL_TABLE_PATH = "//some/yt/categories/path/2021-03-03";

    private static final LocalDate LOCAL_DATE = LocalDate.of(2021, 3, 3);
    private static final Map<String, String> SCHEMA = ImmutableMap.<String, String>builder()
            .put("clid", "int64")
            .put("vid", "string")
            .put("promocode", "string")
            .put("category_name", "string")
            .put("items_count", "int64")
            .put("category_id", "int64")
            .put("partner_payment_no_vat", "double")
            .put("partner_payment_no_vat_max", "double")
            .put("items_billed_cost", "double")
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
    private DistributionOrderStatsService distributionOrderStatsService;

    @Mock
    private StepEventRestClient stepEventRestClient;

    private CategoryAggregateYtExportService ytExportService;

    @BeforeEach
    void setup() {
        setupYtMock();
        this.ytExportService =
                new CategoryAggregateYtExportService(
                ytMockHahn,
                ytMockArnold,
                TABLE_PATH,
                distributionOrderStatsService,
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

        Mockito.when(distributionOrderStatsService.getDistributionOrderStatsAggregateByCategory(any()))
                .thenReturn(getApprovedDistributionOrderStatsCategoryAggregateYtExportDtos());
    }

    @Test
    void testExport() {
        ytExportService.export(LOCAL_DATE);

        final var schemaCaptor = ArgumentCaptor.forClass(MapF.class);

        verifyCreate(schemaCaptor, FULL_TABLE_PATH, cypressMockHahn);
        verifyCreate(schemaCaptor, FULL_TABLE_PATH, cypressMockArnold);

        Mockito.verifyNoMoreInteractions(cypressMockHahn);
        Mockito.verifyNoMoreInteractions(cypressMockArnold);

        verifySchema(schemaCaptor.getAllValues());

        final var entriesCaptor = ArgumentCaptor.forClass(Iterator.class);


        verifyWrite(entriesCaptor, FULL_TABLE_PATH, ytTablesMockArnold);
        verifyWrite(entriesCaptor, FULL_TABLE_PATH, ytTablesMockHahn);

        Mockito.verifyNoMoreInteractions(ytTablesMockHahn);
        Mockito.verifyNoMoreInteractions(ytTablesMockArnold);

        final var exportedEntities = entriesCaptor.getAllValues();

        assertThat(exportedEntities).hasSize(2);

        final var exportedEntity1 = ImmutableList.copyOf(exportedEntities.get(0));
        final var exportedEntity2 = ImmutableList.copyOf(exportedEntities.get(1));

        assertEquals(exportedEntity1.size(), 3);
        assertEquals(exportedEntity2.size(), 3);

        String x = exportedEntity1.toString();
        System.out.println(x);
        String approved = StringTestUtil.getString(
                getClass(),
                "json/DistributionOrderStatsCategoryAggregateYtExportServiceTest.testExport.created.json"
        );
        assertJsonEquals(
                approved,
                exportedEntity1.toString()
        );
        assertJsonEquals(
                approved,
                exportedEntity2.toString()
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
        for (var value : actualSchema) {
            final YTreeListNode schemaActual = (YTreeListNode) value.getOrThrow("schema");
            assertEquals(schemaActual.size(), SCHEMA.size());

            for (YTreeNode field : schemaActual) {
                final var fieldName = field.mapNode().getString("name");
                assertTrue(SCHEMA.containsKey(fieldName) | SCHEMA.containsKey(fieldName));
                final var fieldType = field.mapNode().getString("type");
                assertEquals(SCHEMA.get(fieldName), fieldType);
            }
        }
    }

    private List<DistributionOrderStatsDimensionAggregate> getApprovedDistributionOrderStatsCategoryAggregateYtExportDtos() {
        return Arrays.asList(
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(1L)
                        .setVid("a")
                        .setPromocode("BEBEBE")
                        .setCategoryId(1L)
                        .setCategoryName("a")
                        .setItemsCount(2)
                        .setItemsBilledCost(BigDecimal.ONE)
                        .setPartnerPayment(BigDecimal.ONE)
                        .setPartnerPaymentMax(BigDecimal.ONE)
                        .build(),
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(1L)
                        .setVid("a")
                        .setCategoryId(12L)
                        .setCategoryName("a")
                        .setItemsCount(4)
                        .setItemsBilledCost(BigDecimal.valueOf(2d))
                        .setPartnerPayment(BigDecimal.valueOf(2d))
                        .setPartnerPaymentMax(BigDecimal.valueOf(2d))
                        .build(),
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(1L)
                        .setVid("a")
                        .setCategoryId(123L)
                        .setCategoryName("a")
                        .setItemsCount(3)
                        .setItemsBilledCost(BigDecimal.valueOf(3d))
                        .setPartnerPayment(BigDecimal.valueOf(3d))
                        .setPartnerPaymentMax(BigDecimal.valueOf(3d))
                        .build()
        );
    }
}
