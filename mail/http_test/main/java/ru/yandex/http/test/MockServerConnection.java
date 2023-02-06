package ru.yandex.http.test;

import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.HttpContext;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.http.util.server.LoggingServerConnection;
import ru.yandex.stater.RequestInfo;
import ru.yandex.util.timesource.TimeSource;

public class MockServerConnection
    extends DefaultBHttpServerConnection
    implements LoggingServerConnection
{
    private final long requestStartTime;

    public MockServerConnection() {
        this(TimeSource.INSTANCE.currentTimeMillis());
    }

    public MockServerConnection(final long ts) {
        super(1);

        this.requestStartTime = ts;
    }

    @Override
    public void setRequestContext(
        final Logger logger,
        final Consumer<RequestInfo> stater,
        final GenericAutoCloseable<RuntimeException> resourcesReleaser,
        final HttpContext context)
    {
    }

    @Override
    public void setStater(final Consumer<RequestInfo> stater) {
    }

    @Override
    public void setSessionInfo(final String name, final Object value) {
    }

    @Override
    public long requestStartTime() {
        return requestStartTime;
    }
}
