package ru.yandex.market.request.datasource.trace;

import ru.yandex.market.request.trace.RequestLogRecordBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MockLogger implements Consumer<RequestLogRecordBuilder> {
    private List<RequestLogRecordBuilder> builders = new ArrayList<>();;
    @Override
    public void accept(RequestLogRecordBuilder builder) {
        builders.add(builder);
    }

    public RequestLogRecordBuilder get() {
        if (builders.size() != 1) {
            throw new AssertionError("Expected single call on logger. Actual is " + builders.size() + "\n. " +
                    "Requests: " + builders);
        }
        return builders.get(0);
    }

    public List<RequestLogRecordBuilder> getAll() {
        return builders;
    }
}
