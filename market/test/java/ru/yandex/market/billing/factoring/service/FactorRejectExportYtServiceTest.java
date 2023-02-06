package ru.yandex.market.billing.factoring.service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import ru.yandex.market.billing.factoring.dao.FactorRejectDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FactorRejectExportYtServiceTest extends FunctionalTest {
    private static final String TABLE_PATH = "//some/yt/path";
    private static final Map<String, String> SCHEMA = ImmutableMap.<String, String>builder()
            .put("name", "string")
            .put("inn", "string")
            .put("ogrn", "string")
            .put("status", "string")
            .put("factor", "string")
            .put("rejectDate", "string")
            .put("endRejectDate", "string")
            .put("ticketCkk", "string")
            .build();
    @Autowired
    public FactorRejectDao factorRejectDao;
    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> entitiesCaptor;
    @Captor
    private ArgumentCaptor<MapF> schemaCaptor;
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

    private FactorRejectExportYtService factorRejectExportYtService;

    @BeforeEach
    void setup() {
        setupYtMock();
        this.factorRejectExportYtService = new FactorRejectExportYtService(
                ytMock,
                TABLE_PATH,
                factorRejectDao
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
    @DbUnitDataSet(before = "FactorRejectExportYtServiceTest.testExport.before.csv")
    void testExport() throws JsonProcessingException {
        factorRejectExportYtService.export();

        verifyCreate(schemaCaptor, TABLE_PATH);
        Mockito.verifyNoMoreInteractions(cypressMock);

        verifySchema(schemaCaptor.getAllValues());

        verifyWrite(entitiesCaptor, TABLE_PATH);
        Mockito.verifyNoMoreInteractions(ytTablesMock);

        List<String> actual = ImmutableList.copyOf(entitiesCaptor.getValue()).stream()
                .map(JsonNode::toString)
                .collect(Collectors.toList());

        assertEquals(actual.size(), 9);

        String expectedJson = StringTestUtil.getString(
                getClass(),
                "FactorRejectExportYtServiceTest.testExport.before.json"
        );
        String[] expected = Arrays.stream(new ObjectMapper().readValue(expectedJson, JsonNode[].class))
                .map(JsonNode::toString)
                .toArray(String[]::new);
        assertThat(actual, Matchers.containsInAnyOrder(expected));
    }

    private void verifyWrite(ArgumentCaptor<Iterator<JsonNode>> entitiesCaptor, String tablePath) {
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
