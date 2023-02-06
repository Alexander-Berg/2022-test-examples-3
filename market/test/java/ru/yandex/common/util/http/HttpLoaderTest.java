package ru.yandex.common.util.http;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.MimeType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit-тесты для {@link HttpLoader}.
 *
 * @author Vladislav Bauer
 */
public class HttpLoaderTest extends AbstractHttpTest {

    @Test
    public void testLoadHttpPositive() throws Exception {
        stubFor(get(urlEqualTo(HANDLER))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MimeType.PLAIN.toString())
                .withBody(CONTENT)));

        final String uri = getHandlerUri(HANDLER);
        final String actualContent = HttpLoader.loadHttp(uri, null, 0);

        assertEquals(CONTENT, actualContent);
    }

    @Test
    public void testLoadHttpNegative() throws Exception {
        stubFor(get(urlEqualTo(HANDLER))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        final String uri = getHandlerUri(HANDLER);
        final String actualContent = HttpLoader.loadHttp(uri, null, 5);

        assertNull(actualContent);
    }

}
