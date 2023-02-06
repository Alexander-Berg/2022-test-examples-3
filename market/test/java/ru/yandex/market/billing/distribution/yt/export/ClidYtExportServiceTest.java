package ru.yandex.market.billing.distribution.yt.export;

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
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.distribution.dao.ClidDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mbi.util.MbiAsserts.assertJsonEquals;

@ExtendWith(MockitoExtension.class)
class ClidYtExportServiceTest extends FunctionalTest {

    private static final String TABLE_PATH = "//some/yt/path";
    private static final Map<String, String> SCHEMA = ImmutableMap.<String, String>builder()
            .put("clid", "int64")
            .put("partnerSegment", "string")
            .build();
    @Autowired
    public ClidDao distributionClientsPgDao;
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
    private ClidYtExportService clidYtExportService;

    @BeforeEach
    void setup() {
        setupYtMock();
        this.clidYtExportService = new ClidYtExportService(
                ytMock,
                TABLE_PATH,
                distributionClientsPgDao
        );
    }

    private void setupYtMock() {
        final var transactionId = Mockito.mock(GUID.class);
        Mockito.when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);
        Mockito.when(transaction.getId())
                .thenReturn(transactionId);

        Mockito.when(ytMock.tables()).thenReturn(ytTablesMock);
        Mockito.when(ytMock.cypress()).thenReturn(cypressMock);
        Mockito.when(ytMock.transactions()).thenReturn(ytTransactions);
    }

    @Test
    @DbUnitDataSet(before = "db/ClidYtExportServiceTest.testExport.before.csv")
    void testExport() {
        clidYtExportService.export();

        final var schemaCaptor = ArgumentCaptor.forClass(MapF.class);

        verifyCreate(schemaCaptor, TABLE_PATH);
        Mockito.verifyNoMoreInteractions(cypressMock);

        verifySchema(schemaCaptor.getAllValues());

        final var entitiesCaptor = ArgumentCaptor.forClass(Iterator.class);

        verifyWrite(entitiesCaptor, TABLE_PATH);
        Mockito.verifyNoMoreInteractions(ytTablesMock);

        final var exportedEntities = ImmutableList.copyOf(entitiesCaptor.getValue());
        assertEquals(exportedEntities.size(), 1);

        String expected = StringTestUtil.getString(
                getClass(),
                "json/ClidYtExportServiceTest.testExport.before.json"
        );
        assertJsonEquals(expected, exportedEntities.toString());
    }

    private void verifyWrite(ArgumentCaptor<Iterator> entitiesCaptor, String tablePath) {
        Mockito.verify(ytTablesMock)
                .write(
                        any(), anyBoolean(),
                        eq(YPath.simple(tablePath)),
                        eq(YTableEntryTypes.JACKSON_UTF8),
                        entitiesCaptor.capture()
                );
    }


    private void verifyCreate(ArgumentCaptor<MapF> schemaCaptor, String tablePath) {
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
                assertTrue(SCHEMA.containsKey(fieldName));
                final var fieldType = field.mapNode().getString("type");
                assertEquals(SCHEMA.get(fieldName), fieldType);
            }
        }
    }


}
