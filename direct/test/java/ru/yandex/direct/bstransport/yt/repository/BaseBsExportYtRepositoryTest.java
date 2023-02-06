package ru.yandex.direct.bstransport.yt.repository;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.bstransport.yt.TestMessage;
import ru.yandex.direct.ytcomponents.config.BsExportYtDynConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.yt.rpcproxy.ERowModificationType;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnSortOrder;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.bstransport.yt.repository.ColumnMapping.column;
import static ru.yandex.yt.rpcproxy.ERowModificationType.RMT_DELETE;
import static ru.yandex.yt.rpcproxy.ERowModificationType.RMT_WRITE;


class BaseBsExportYtRepositoryTest {
    private CaesarQueueRequestFactory caesarQueueRequestFactory;
    private BsExportYtRepositoryContext context;

    @BeforeEach
    void before() {
        var ytProvider = mock(YtProvider.class);
        var ytDynamicOperator = mock(YtDynamicOperator.class);
        when(ytProvider.getDynamicOperator(any())).thenReturn(ytDynamicOperator);
        var config = mock(BsExportYtDynConfig.class);
        when(config.getTransactionTimeout()).thenReturn(Duration.ZERO);
        caesarQueueRequestFactory = mock(CaesarQueueRequestFactory.class);
        context = new BsExportYtRepositoryContext(ytProvider, config, caesarQueueRequestFactory);
    }

    @Test
    void testModifyRows() {
        var testMessage = getTestMessage();
        var queueRequest = new ModifyRowsRequest("QueuePath", new TableSchema.Builder().setUniqueKeys(false).build());
        when(caesarQueueRequestFactory.getRequest(any(), any())).thenReturn(queueRequest);

        var testRepository = spy(new TestBsExportYtRepository(context, true));
        testRepository.modify(List.of(testMessage));

        ArgumentCaptor<ModifyRowsRequest> tableRequestCapture = ArgumentCaptor.forClass(ModifyRowsRequest.class);
        ArgumentCaptor<ModifyRowsRequest> queueRequestCapture = ArgumentCaptor.forClass(ModifyRowsRequest.class);

        verify(testRepository).doTransactionRequest(tableRequestCapture.capture(), queueRequestCapture.capture());

        assertThat(tableRequestCapture.getAllValues()).hasSize(1);
        var gotTableRequest = tableRequestCapture.getAllValues().get(0);
        List<UnversionedValue> expectedValues = List.of(
                new UnversionedValue(0, ColumnValueType.UINT64, false, 12L),
                new UnversionedValue(1, ColumnValueType.STRING, false, "test data".getBytes()),
                new UnversionedValue(2, ColumnValueType.UINT64, false, 12345L),
                new UnversionedValue(2, ColumnValueType.NULL, false, null)
        );
        checkTableRequest(gotTableRequest, RMT_WRITE, expectedValues);

        assertThat(queueRequestCapture.getAllValues()).hasSize(1);
        assertThat(queueRequestCapture.getAllValues().get(0)).isEqualTo(queueRequest);
    }

    @Test
    void testDeleteRows() {
        var testMessage = getTestMessage();
        var queueRequest = new ModifyRowsRequest("QueuePath", new TableSchema.Builder().setUniqueKeys(false).build());
        when(caesarQueueRequestFactory.getRequest(any(), any())).thenReturn(queueRequest);

        var testRepository = spy(new TestBsExportYtRepository(context, true));
        testRepository.delete(List.of(testMessage));

        ArgumentCaptor<ModifyRowsRequest> tableRequestCapture = ArgumentCaptor.forClass(ModifyRowsRequest.class);
        ArgumentCaptor<ModifyRowsRequest> queueRequestCapture = ArgumentCaptor.forClass(ModifyRowsRequest.class);
        verify(testRepository).doTransactionRequest(tableRequestCapture.capture(), queueRequestCapture.capture());

        assertThat(tableRequestCapture.getAllValues()).hasSize(1);
        var gotTableRequest = tableRequestCapture.getAllValues().get(0);
        List<UnversionedValue> expectedValues = List.of(
                new UnversionedValue(0, ColumnValueType.UINT64, false, 12L)
        );
        checkTableRequest(gotTableRequest, RMT_DELETE, expectedValues);

        assertThat(queueRequestCapture.getAllValues()).hasSize(1);
        assertThat(queueRequestCapture.getAllValues().get(0)).isEqualTo(queueRequest);
    }

    private TestMessage getTestMessage() {
        return TestMessage.newBuilder()
                .setId(12L)
                .setData("test data")
                .setIterId(12345L)
                .build();
    }

    private void checkTableRequest(ModifyRowsRequest gotTableRequest, ERowModificationType modificationType,
                                   List<UnversionedValue> expectedValues) {
        assertThat(gotTableRequest.getRowModificationTypes()).hasSize(1);
        assertThat(gotTableRequest.getRowModificationTypes().get(0)).isEqualTo(modificationType);
        assertThat(gotTableRequest.getPath()).isEqualTo("TestTable");
        assertThat(gotTableRequest.getRows()).hasSize(1);
        assertThat(gotTableRequest.getRows().get(0).getValues()).usingElementComparatorIgnoringFields("id").containsExactlyInAnyOrder(expectedValues.toArray(new UnversionedValue[0]));

    }

    private static class TestBsExportYtRepository extends BaseBsExportYtRepository<TestMessage> {
        public TestBsExportYtRepository(BsExportYtRepositoryContext context, boolean withQueue) {
            super(context, "TestTable", "TestQueue");
        }

        @Override
        public List<ColumnMapping<TestMessage>> getSchemaWithMapping() {
            return List.of(
                    column(new ColumnSchema("ID", ColumnValueType.UINT64, ColumnSortOrder.ASCENDING),
                            TestMessage::getId),
                    column(new ColumnSchema("Data", ColumnValueType.STRING), TestMessage::getData),
                    column(new ColumnSchema("IterID", ColumnValueType.UINT64), TestMessage::getIterId),
                    column(new ColumnSchema("NullField", ColumnValueType.UINT64), message -> null)
            );
        }
    }
}
