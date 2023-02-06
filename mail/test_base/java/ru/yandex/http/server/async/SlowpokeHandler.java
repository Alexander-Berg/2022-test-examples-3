package ru.yandex.http.server.async;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

public class SlowpokeHandler implements HttpAsyncRequestHandler<Object> {
    private static final Timer TIMER = new Timer(true);
    private final HttpAsyncRequestHandler<Object> next;
    private final long delay;

    @SuppressWarnings("unchecked")
    SlowpokeHandler(
        final HttpAsyncRequestHandler<?> next,
        final long delay)
    {
        this.next = (HttpAsyncRequestHandler<Object>) next;
        this.delay = delay;
    }

    @Override
    public HttpAsyncRequestConsumer<Object> processRequest(
        final HttpRequest request,
        final HttpContext context)
        throws HttpException, IOException
    {
        return next.processRequest(request, context);
    }

    @Override
    public void handle(
        final Object request,
        final HttpAsyncExchange exchange,
        final HttpContext context)
    {
        TIMER.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    try {
                        next.handle(request, exchange, context);
                    } catch (HttpException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            delay);
    }
}

