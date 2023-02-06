package ru.yandex.common.util.http;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit-тесты для {@link HttpClientUtils}.
 *
 * @author Vladislav Bauer
 */
public class HttpClientUtilsTest {

    @Test
    public void testCreatePostMethod() throws Exception {
        final String uri = "http://localhost:123/test";
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
            .put("a", "b")
            .put("c", "4")
            .build();

        final HttpPost method = HttpClientUtils.createPostMethod(uri, StandardCharsets.UTF_8.name(), params);
        assertEquals(uri, method.getURI().toString());
        assertTrue(method.getEntity() instanceof UrlEncodedFormEntity);
    }

    @Test
    public void testCreateHttpClient() {
        final int connectTimeout = RandomUtils.nextInt();
        final int soTimeout = RandomUtils.nextInt();

        final HttpClient httpClient = HttpClientUtils.createHttpClient(connectTimeout, soTimeout);
        final HttpParams params = httpClient.getParams();

        checkHttpParams(params, connectTimeout, soTimeout);
    }

    @Test
    public void testCreateHttpParamsPositive() {
        final int connectTimeout = RandomUtils.nextInt();
        final int soTimeout = RandomUtils.nextInt();
        final HttpParams params = HttpClientUtils.createHttpParams(connectTimeout, soTimeout);
        checkHttpParams(params, connectTimeout, soTimeout);
    }

    @Test
    public void testCreateHttpParamsNegative() {
        final HttpParams params = HttpClientUtils.createHttpParams(0, -1);
        checkHttpParams(params, 0, 0);
    }


    private void checkHttpParams(final HttpParams params, final int connectTimeout, final int soTimeout) {
        assertEquals(connectTimeout, HttpConnectionParams.getConnectionTimeout(params));
        assertEquals(soTimeout, HttpConnectionParams.getSoTimeout(params));
    }

}
