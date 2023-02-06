package ru.yandex.direct.jobs.directdb.helper;

import java.sql.ResultSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.yql.ResultSetFuture;

@ParametersAreNonnullByDefault
public class TestErrorResultSetFuture implements ResultSetFuture {
    @Override
    public String getOperationId() {
        return "operation-id";
    }

    @Override
    public String getSql() {
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public ResultSet get() throws ExecutionException {
        throw new ExecutionException("Error", new RuntimeException());
    }

    @Override
    public ResultSet get(long timeout, TimeUnit unit) {
        return null;
    }
}
