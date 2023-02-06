package ru.yandex.search.messenger.indexer;

import java.io.IOException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.ServerException;
import ru.yandex.parser.uri.CgiParams;

public class SelectByServiceHandler
    implements HttpRequestHandler
{
    private final Map<String, HttpRequestHandler> handlers;

    public SelectByServiceHandler(
        final Map<String, HttpRequestHandler> handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        String service = null;
        Header serviceHeader = request.getFirstHeader("service");
        if (serviceHeader != null) {
            service = serviceHeader.getValue();
        }

        if (service == null) {
            service = new CgiParams(request).getString("service");
        }

        HttpRequestHandler handler = handlers.get(service);
        if (handler == null) {
            throw new ServerException(
                HttpStatus.SC_NOT_IMPLEMENTED,
                "Service not found " + service);
        }

        handler.handle(request, response, context);
    }
}
