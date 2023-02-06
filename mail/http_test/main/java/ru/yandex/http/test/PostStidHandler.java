package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.uri.CgiParams;

public class PostStidHandler implements HttpRequestHandler {
    private StaticServer lenulca;
    private final HttpHost host;
    private final String uri;
    private final char separator;

    public PostStidHandler(
        final StaticServer lenulca,
        final HttpHost host,
        final String uri)
    {
        this.lenulca = lenulca;
        this.host = host;
        this.uri = uri;
        if (uri.indexOf('?') == -1) {
            separator = '?';
        } else {
            separator = '&';
        }
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
            CloseableHttpResponse lenulcaResponse =
                client.execute(new HttpGet(lenulca.host() + "/get/" + stid)))
        {
            int status = lenulcaResponse.getStatusLine().getStatusCode();
            HttpEntity entity = lenulcaResponse.getEntity();
            ByteArrayEntity nextEntity =
                new ByteArrayEntity(CharsetUtils.toByteArray(entity));
            nextEntity.setContentType(entity.getContentType());
            if (status == HttpStatus.SC_OK) {
                HttpPost post =
                    new HttpPost(host + uri + separator + "stid=" + stid);
                post.setEntity(nextEntity);
                try (CloseableHttpResponse nextResponse =
                        client.execute(post))
                {
                    status = nextResponse.getStatusLine().getStatusCode();
                    entity = nextResponse.getEntity();
                    nextEntity =
                        new ByteArrayEntity(CharsetUtils.toByteArray(entity));
                    nextEntity.setContentType(entity.getContentType());
                }
            }
            response.setStatusCode(status);
            response.setEntity(nextEntity);
        }
    }
}

