package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.uri.CgiParams;

public class TikaiteProxyHandler implements HttpRequestHandler {
    private final HttpHost tikaiteHost;

    public TikaiteProxyHandler(final HttpHost tikaiteHost) {
        this.tikaiteHost = tikaiteHost;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        CgiParams params = new CgiParams(request);
        String stid = params.getString("stid");
        try (CloseableHttpClient client = Configs.createDefaultClient();
            CloseableHttpResponse nextResponse =
                client.execute(
                    new HttpGet(tikaiteHost + "/get/" + stid + "?name=mail")))
        {
            response.setStatusCode(
                nextResponse.getStatusLine().getStatusCode());
            HttpEntity entity = nextResponse.getEntity();
            ByteArrayEntity responseEntity =
                new ByteArrayEntity(CharsetUtils.toByteArray(entity));
            responseEntity.setContentType(entity.getContentType());
            response.setEntity(responseEntity);
        }
    }
}

