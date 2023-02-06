package ru.yandex.travel.commons.logging.ydb;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.yandex.ydb.OperationProtos;
import com.yandex.ydb.StatusCodesProtos.StatusIds.StatusCode;
import com.yandex.ydb.core.Result;
import com.yandex.ydb.core.UnexpectedResultException;
import com.yandex.ydb.core.auth.AuthProvider;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.core.rpc.OperationTray;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.YdbTable;
import com.yandex.ydb.table.rpc.TableRpc;
import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.logging.ydb.TOrderLogRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YdbLogTableClientTest {
    private static int OPERATION_ID_COUNTER = 0;
    private static int QUERY_ID_COUNTER = 0;

    @Test
    public void testSimpleHappyPath() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any())).thenReturn(executeQuerySuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(1)).createSession(any(), any());
        verify(tableRpc, times(1)).executeDataQuery(any(), any());
    }

    @Test
    public void testSessionRetry() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenReturn(createSessionFailure(StatusCode.OVERLOADED))
                .thenReturn(createSessionFailure(StatusCode.ABORTED))
                .thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any())).thenReturn(executeQuerySuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(3)).createSession(any(), any());
        verify(tableRpc, times(1)).executeDataQuery(any(), any());
    }

    @Test
    public void testSessionTooManyReties() {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE));

        assertThatThrownBy(() ->
                client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasMessageContaining("failed to insert the messages, code: UNAVAILABLE");

        verify(tableRpc, times(10)).createSession(any(), any());
    }

    @Test
    public void testSessionNonRetryableResponse() {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenReturn(createSessionFailure(StatusCode.UNAUTHORIZED));

        assertThatThrownBy(() -> client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build()))
                .get(5, TimeUnit.SECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasMessageContaining("failed to insert the messages, code: UNAUTHORIZED");

        verify(tableRpc, times(1)).createSession(any(), any());
    }

    @Test
    public void testSessionNoRetriesOnInternalError() {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenThrow(new RuntimeException("Something went wrong in the client side"));

        assertThatThrownBy(() -> client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build()))
                .get(5, TimeUnit.SECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasMessageContaining("failed to insert the messages: cannot acquire session from pool");

        verify(tableRpc, times(1)).createSession(any(), any());
    }

    @Test
    public void testQueryRetry() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any()))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQuerySuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(1)).createSession(any(), any());
        verify(tableRpc, times(3)).executeDataQuery(any(), any());
    }

    @Test
    public void testBadSessionRetry() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any()))
                // the response causes current session deletion and creation of a new one
                .thenReturn(executeQueryFailure(StatusCode.BAD_SESSION))
                .thenReturn(executeQuerySuccess());
        when(tableRpc.deleteSession(any(), any())).thenReturn(deleteSessionSuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(2)).createSession(any(), any());
        verify(tableRpc, times(2)).executeDataQuery(any(), any());
        verify(tableRpc, times(1)).deleteSession(any(), any());
    }

    @Test
    public void testExecuteRetry() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any()))
                .thenThrow(new UnexpectedResultException("client error", com.yandex.ydb.core.StatusCode.CLIENT_RESOURCE_EXHAUSTED))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQuerySuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(1)).createSession(any(), any());
        verify(tableRpc, times(3)).executeDataQuery(any(), any());
    }

    @Test
    public void testPreparedQueriesCache() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);
        AtomicInteger preparedQueries = new AtomicInteger(0);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any())).thenAnswer(invocation -> {
            YdbTable.ExecuteDataQueryRequest request = invocation.getArgument(0);
            if (!Strings.isNullOrEmpty(request.getQuery().getId())) {
                preparedQueries.incrementAndGet();
            }
            return executeQuerySuccess();
        });

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);
        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);
        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(1)).createSession(any(), any());
        verify(tableRpc, times(3)).executeDataQuery(any(), any());
        assertThat(preparedQueries.get()).isEqualTo(2);
    }

    @Test
    public void testPreparedQueryNotFoundRetry() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);
        AtomicInteger queryIndex = new AtomicInteger(0);
        AtomicInteger preparedQueries = new AtomicInteger(0);

        when(tableRpc.createSession(any(), any())).thenReturn(createSessionSuccess());
        var queryResponses = List.of(
                executeQuerySuccess(),
                // the second request is a prepared query but this reply will invalidate it
                executeQueryFailure(StatusCode.NOT_FOUND),
                executeQuerySuccess()
        );
        when(tableRpc.executeDataQuery(any(), any())).thenAnswer(invocation -> {
            YdbTable.ExecuteDataQueryRequest request = invocation.getArgument(0);
            if (!Strings.isNullOrEmpty(request.getQuery().getId())) {
                preparedQueries.incrementAndGet();
            }
            var resultIdx = Math.min(queryIndex.getAndIncrement(), queryResponses.size() - 1);
            return queryResponses.get(resultIdx);
        });

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);
        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(1)).createSession(any(), any());
        verify(tableRpc, times(3)).executeDataQuery(any(), any());
        assertThat(preparedQueries.get()).isEqualTo(1);
    }

    @Test
    public void testMaxAttempts() throws Exception {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionSuccess())
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any()))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQueryFailure(StatusCode.BAD_SESSION))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQuerySuccess());
        when(tableRpc.deleteSession(any(), any()))
                .thenReturn(deleteSessionSuccess());

        client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build())).get(5, TimeUnit.SECONDS);

        verify(tableRpc, times(8)).createSession(any(), any());
        verify(tableRpc, times(4)).executeDataQuery(any(), any());
        verify(tableRpc, times(1)).deleteSession(any(), any());
    }

    @Test
    public void testTooManyAttempts() {
        TableRpc tableRpc = mockRpc();
        YdbLogTableClient client = logClient(tableRpc);

        when(tableRpc.createSession(any(), any()))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionSuccess())
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionFailure(StatusCode.UNAVAILABLE))
                .thenReturn(createSessionSuccess());
        when(tableRpc.executeDataQuery(any(), any()))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQueryFailure(StatusCode.BAD_SESSION))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQueryFailure(StatusCode.UNAVAILABLE))
                .thenReturn(executeQuerySuccess());
        when(tableRpc.deleteSession(any(), any()))
                .thenReturn(deleteSessionSuccess());

        assertThatThrownBy(() -> client.insertLogRecords(List.of(TOrderLogRecord.newBuilder().build()))
                .get(5, TimeUnit.SECONDS))
                .isExactlyInstanceOf(ExecutionException.class)
                .hasMessageContaining("failed to insert the messages, code: UNAVAILABLE");

        verify(tableRpc, times(8)).createSession(any(), any());
        verify(tableRpc, times(4)).executeDataQuery(any(), any());
        verify(tableRpc, times(1)).deleteSession(any(), any());
    }

    @SneakyThrows
    private static TableRpc mockRpc() {
        TableRpc tableRpc = Mockito.mock(TableRpc.class);

        // the tray is needed for async operations completion
        Constructor ctor = Class.forName("com.yandex.ydb.core.grpc.GrpcOperationTray")
                .getDeclaredConstructor(GrpcTransport.class);
        ctor.setAccessible(true);
        OperationTray operationTray = (OperationTray) ctor.newInstance(new Object[]{null});
        when(tableRpc.getOperationTray()).thenReturn(operationTray);

        return tableRpc;
    }

    private static YdbLogTableClient logClient(TableRpc tableRpc) {
        int maxAttempts = 10;
        int backoffCeiling = 2;
        return new YdbLogTableClient(
                new FakeAuthProvider(),
                TableClient.newClient(tableRpc)
                        // not implemented yet
                        //.sessionCreationMaxRetries(maxAttempts)
                        .build(),
                "table",
                Duration.ofSeconds(1),
                maxAttempts,
                Duration.ofMillis(10),
                backoffCeiling
        );
    }

    private static CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSessionSuccess() {
        YdbTable.CreateSessionResult result = YdbTable.CreateSessionResult.newBuilder().setSessionId("some sid").build();
        YdbTable.CreateSessionResponse response = YdbTable.CreateSessionResponse.newBuilder()
                .setOperation(operation(StatusCode.SUCCESS, result))
                .build();
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private static CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSessionFailure(StatusCode statusCode) {
        YdbTable.CreateSessionResponse response = YdbTable.CreateSessionResponse.newBuilder()
                .setOperation(operation(statusCode, null))
                .build();
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private static CompletableFuture<Result<YdbTable.ExecuteDataQueryResponse>> executeQuerySuccess() {
        YdbTable.ExecuteQueryResult result = YdbTable.ExecuteQueryResult.newBuilder()
                .setQueryMeta(YdbTable.QueryMeta.newBuilder()
                        .setId("q" + QUERY_ID_COUNTER++)
                        .build())
                .build();
        YdbTable.ExecuteDataQueryResponse response = YdbTable.ExecuteDataQueryResponse.newBuilder()
                .setOperation(operation(StatusCode.SUCCESS, result))
                .build();
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private static CompletableFuture<Result<YdbTable.ExecuteDataQueryResponse>> executeQueryFailure(StatusCode statusCode) {
        YdbTable.ExecuteDataQueryResponse response = YdbTable.ExecuteDataQueryResponse.newBuilder()
                .setOperation(operation(statusCode, null))
                .build();
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private static CompletableFuture<Result<YdbTable.DeleteSessionResponse>> deleteSessionSuccess() {
        YdbTable.DeleteSessionResponse response = YdbTable.DeleteSessionResponse.newBuilder()
                .setOperation(operation(StatusCode.SUCCESS, null))
                .build();
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private static OperationProtos.Operation operation(StatusCode statusCode, Message result) {
        OperationProtos.Operation.Builder operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setStatus(statusCode)
                .setId("op_ " + OPERATION_ID_COUNTER++);
        if (result != null) {
            operation.setResult(Any.pack(result));
        }
        return operation.build();
    }

    private static class FakeAuthProvider implements AuthProvider {
        @Override
        public String getToken() {
            return "token";
        }

        @Override
        public void close() {
        }
    }
}
