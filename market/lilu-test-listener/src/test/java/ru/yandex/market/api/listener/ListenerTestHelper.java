package ru.yandex.market.api.listener;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dimkarp93
 */
public class ListenerTestHelper {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static HttpGet get(URI uri) {
        return new HttpGet(uri);
    }

    public static HttpPost post(URI uri, String body) {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(bytes(body)));

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(entity);

        return httpPost;
    }

    public static byte[] bytes(String string) {
        return string.getBytes(UTF8);
    }

    public static String readAll(Reader reader) {
        try {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static URI uri(String host,
                          int port,
                          String path,
                          Map<? extends String, ? extends String> params) {
        try {
            String query =
                null == params ?
                    "" :
                    params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            return new URI("http",
                null,
                host,
                port,
                path,
                query,
                null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}
