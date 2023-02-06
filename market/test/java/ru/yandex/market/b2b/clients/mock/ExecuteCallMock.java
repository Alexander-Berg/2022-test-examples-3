package ru.yandex.market.b2b.clients.mock;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.request.trace.Module;

public class ExecuteCallMock<R> extends ExecuteCall<R, RetryStrategy> {
    private static final Logger TRACE_LOGGER = LoggerFactory.getLogger(ExecuteCallMock.class);

    private R result;

    public ExecuteCallMock(R result) {
        super(new CallMock<>(result), TRACE_LOGGER, RetryStrategy.NO_RETRY_STRATEGY, null, null);
        this.result = result;
    }

    protected ExecuteCallMock(@Nonnull Call<R> call, @Nonnull Logger traceLogger, @Nonnull RetryStrategy retryStrategy,
                              @Nullable Module sourceModule, @Nullable Module targetModule) {
        super(call, traceLogger, retryStrategy, sourceModule, targetModule);
    }

    @Override
    public CompletableFuture<R> schedule() {
        CompletableFuture<R> future = new CompletableFuture<>();
        future.complete(result);
        return future;
    }

    @Override
    public CompletableFuture<Void> scheduleVoid() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }
}
