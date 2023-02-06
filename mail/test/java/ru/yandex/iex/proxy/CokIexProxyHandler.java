package ru.yandex.iex.proxy;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.function.ByteArrayCopier;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.server.sync.JsonEntity;
import ru.yandex.http.test.Configs;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonString;
import ru.yandex.parser.uri.CgiParams;

public class CokIexProxyHandler implements HttpRequestHandler {
    private static final String GETBODY = "getbody";

    private final Random random = new Random();
    private final HttpHost coke;
    private final String iexUrl;

    public CokIexProxyHandler(final int cokePort, final String iexUrl) {
        coke = new HttpHost("localhost", cokePort);
        this.iexUrl = iexUrl;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        HttpRequest cokeRequest =
            new BasicHttpRequest(request.getRequestLine());
        try (CloseableHttpClient client = Configs.createDefaultClient();
            CloseableHttpResponse cokeResponse =
                client.execute(coke, cokeRequest))
        {
            final String cokeUri = request.getRequestLine().getUri();
            final int q = cokeUri.indexOf('?');
            final String iexParams;
            if (q == -1) {
                iexParams = "";
            } else {
                iexParams = cokeUri.substring(q);
            }
            CgiParams cgiParams = new CgiParams(request);
            final String e = cgiParams.getString("e", "");

            //"getbody" entity is disabled in Tomita's IEXSRV
            if (e.equals(GETBODY)) {
                final JsonMap root =
                    new JsonMap(BasicContainerFactory.INSTANCE);
                final JsonMap getBody =
                    new JsonMap(BasicContainerFactory.INSTANCE);
                root.put(GETBODY, getBody);
                getBody.put(
                    "text",
                    new JsonString(
                        CharsetUtils.toString(cokeResponse.getEntity())));
                response.setEntity(
                    new JsonEntity(root, request));
            } else if (e.contains(GETBODY)) {
                throw new BadRequestException("\"getbody\" entity can't be "
                    + "mixed with others");
            } else {
                ByteArrayEntity cokeEntity =
                    CharsetUtils.toDecodable(cokeResponse.getEntity())
                        .processWith(ByteArrayEntityFactory.INSTANCE);

                HttpPost iexPost = new HttpPost(
                    iexUrl + iexParams
                    + "&coke-proxy-timestamp=" + System.currentTimeMillis()
                    + "&random-key=" + random.nextInt());
                iexPost.setEntity(cokeEntity);
                try (CloseableHttpResponse iexResponse =
                    client.execute(iexPost))
                {
                    HttpEntity iexEntity = iexResponse.getEntity();
                    DecodableByteArrayOutputStream data =
                        CharsetUtils.toDecodable(iexEntity);
                    Logger logger =
                        (Logger) context.getAttribute(BaseHttpServer.LOGGER);
                    logger.fine(
                        "Tomita request: " + iexPost
                        + ", response: " + iexResponse + ", body: "
                        + new String(
                            data.processWith(ByteArrayCopier.INSTANCE),
                            CharsetUtils.contentType(iexEntity).getCharset()));
                    ByteArrayEntity responseEntity =
                        data.processWith(ByteArrayEntityFactory.INSTANCE);
                    response.setStatusLine(iexResponse.getStatusLine());
                    responseEntity.setContentType(iexEntity.getContentType());
                    responseEntity.setContentEncoding(
                        iexEntity.getContentEncoding());
                    response.setEntity(responseEntity);
                }
            }
        }
    }
}

