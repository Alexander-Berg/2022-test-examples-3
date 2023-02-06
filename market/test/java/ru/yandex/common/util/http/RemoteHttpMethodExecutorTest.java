package ru.yandex.common.util.http;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.MimeType;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.http.RemoteHttpMethodExecutor.MethodType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link RemoteHttpMethodExecutor}.
 *
 * @author Vladislav Bauer
 */
public class RemoteHttpMethodExecutorTest extends AbstractHttpTest {

    private static final List<Pair<String, String>> PARAMS = Collections.emptyList();


    private RemoteHttpMethodExecutor executor;


    @Before
    public void onBefore() throws Exception {
        stubFor(get(urlEqualTo(HANDLER))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MimeType.PLAIN.toString())
                .withBody(CONTENT)));

        stubFor(get(urlEqualTo(HANDLER_FAIL_INTERNAL))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        stubFor(get(urlEqualTo(HANDLER_FAIL_IO))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        executor = createExecutor();
    }


    @Test
    public void testExecuteAsString() throws Exception {
        final String content = executor.executeAsString(MethodType.GET, HANDLER, PARAMS);
        assertEquals(CONTENT, content);
    }

    @Test
    public void testExecuteAsStringForPath() throws Exception {
        final String path = getHandlerUri(HANDLER);
        final String content = executor.executeAsStringForPath(MethodType.GET, path, PARAMS);
        assertEquals(CONTENT, content);
    }

    @Test
    public void testExecuteAsStream() throws Exception {
        try (InputStream content = executor.executeAsStream(MethodType.GET, HANDLER, PARAMS)) {
            assertEquals(CONTENT, IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testExecuteAsStreamForPath() throws Exception {
        final String path = getHandlerUri(HANDLER);
        try (InputStream content = executor.executeAsStreamForPath(MethodType.GET, path, PARAMS)) {
            assertEquals(CONTENT, IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStringBadUri() throws Exception {
        fail(executor.executeAsString(MethodType.GET, HANDLER_FAIL_INTERNAL, PARAMS));
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStringForPathBadUri() throws Exception {
        final String path = getHandlerUri(HANDLER_FAIL_INTERNAL);
        fail(executor.executeAsStringForPath(MethodType.GET, path, PARAMS));
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStreamBadUri() throws Exception {
        try (InputStream content = executor.executeAsStream(MethodType.GET, HANDLER_FAIL_INTERNAL, PARAMS)) {
            fail(IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStreamForPathBadUri() throws Exception {
        final String path = getHandlerUri(HANDLER_FAIL_INTERNAL);
        try (InputStream content = executor.executeAsStreamForPath(MethodType.GET, path, PARAMS)) {
            fail(IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStringBadIo() throws Exception {
        fail(executor.executeAsString(MethodType.GET, HANDLER_FAIL_IO, PARAMS));
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStringForPathBadIo() throws Exception {
        final String path = getHandlerUri(HANDLER_FAIL_IO);
        fail(executor.executeAsStringForPath(MethodType.GET, path, PARAMS));
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStreamBadIo() throws Exception {
        try (InputStream content = executor.executeAsStream(MethodType.GET, HANDLER_FAIL_IO, PARAMS)) {
            fail(IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    @Test(expected = RemoteHttpExecuteException.class)
    public void testExecuteAsStreamForPathBadIo() throws Exception {
        final String path = getHandlerUri(HANDLER_FAIL_IO);
        try (InputStream content = executor.executeAsStreamForPath(MethodType.GET, path, PARAMS)) {
            fail(IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }


    private RemoteHttpMethodExecutor createExecutor() throws Exception {
        final RemoteHttpMethodExecutor executor = new RemoteHttpMethodExecutor();
        executor.setHttpPath(getHandlerUri(StringUtils.EMPTY));
        executor.afterPropertiesSet();
        return executor;
    }

}
