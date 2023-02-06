package ru.yandex.iex.proxy;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

public class ProxyPostHandler implements HttpRequestHandler {
    private final Map<String, String> dataToResponse;

    public ProxyPostHandler(final Map<String, String> dataToResponse) {
        this.dataToResponse = dataToResponse;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        String responseString = "";
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity =
                ((HttpEntityEnclosingRequest) request).getEntity();
            String entityString = EntityUtils.toString(entity);
            if (dataToResponse.containsKey(entityString)) {
                responseString = dataToResponse.get(entityString);
            }
        }
        response.setEntity(new StringEntity(responseString));
    }
}

