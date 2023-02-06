package ru.yandex.logbroker.client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.parser.uri.CgiParams;

public abstract class AbstractLogbrokerServer
    extends BaseHttpServer<ImmutableBaseServerConfig>
{
    protected final AtomicInteger errors = new AtomicInteger(0);
    protected final String hostName;

    protected AbstractLogbrokerServer(
        final ImmutableBaseServerConfig config,
        final String hostName)
        throws IOException
    {
        super(config);

        this.hostName = hostName;
        register(new Pattern<>("", true), new UnsupportHandler());
    }

    public String hostName() {
        return hostName;
    }

    public void checkErrors() {
        assert errors.get() == 0;
    }

    // CSOFF: ParameterNumber
    protected boolean handleUnsupport(
        final String hostName,
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        return false;
    }
    // CSON: ParameterNumber

    private class UnsupportHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            Header hostHeader = request.getFirstHeader("Host");
            String host = hostHeader.getValue();
            if (host != null) {
                host = host.split(":")[0];
            }

            if (host != null
                && handleUnsupport(host, request, response, context))
            {
                return;
            }

            errors.incrementAndGet();
            throw new NotImplementedException("Method not supported");
        }
    }

    protected static void checkRequest(final CgiParams params)
        throws HttpException
    {
        if (!params.containsKey(AbstractLogbrokerClient.CLIENT)) {
            throw new BadRequestException("No client id supplied");
        }

        if (!params.containsKey(LogbrokerClient.TOPIC)) {
            throw new BadRequestException("No topic supplied");
        }
    }
}
