package ru.yandex.reminders.logic.panel.old.client;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Voytitsky
 */
public class PanelApiClientTest {

    private static final DateTimeZone TZ = DateTimeZone.forOffsetHours(3);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(TZ);

    @Test
    public void formatRfc1123() {
        DateTime dateTime = FORMATTER.parseDateTime("2013-11-08 22:27:29,678");
        Assert.equals("Fri, 08 Nov 2013 19:27:29 GMT", PanelApiClient.formatRfc1123(dateTime));
    }

    @Test
    public void addNote() throws IOException {
        HttpClient httpClientMock = mock(HttpClient.class);
        HttpResponse httpResponseMock = mock(HttpResponse.class);

        StatusLine httpStatusLineMock = mock(StatusLine.class);
        when(httpStatusLineMock.getStatusCode()).thenReturn(400);

        when(httpResponseMock.getStatusLine()).thenReturn(httpStatusLineMock);

        PanelApiClient panelApiClient = new PanelApiClient("localhost:123", httpClientMock);
        final PassportUid uid = PassportUid.cons(1371858);
        final String service = "blaService";
        final String id = "fooID";
        final String secureKey = "yeKeruces";
        final Instant expires = FORMATTER.parseDateTime("2013-11-22 23:05:29,678").toInstant();
        final byte[] body = "{a=b}".getBytes();

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);

        doReturn(httpResponseMock).when(httpClientMock).execute(
                requestCaptor.capture(), any(ResponseHandler.class), any(HttpContext.class));

        try {
            panelApiClient.addNote(uid, service, id, secureKey, expires, body);
            Assert.fail("expected PanelApiClientException was not thrown");
        } catch (PanelApiClientException e) {
            Assert.isFalse(e.isRetriable());
            Assert.equals("request wasn't completed successfully, status code=400", e.getMessage());
        }

        HttpUriRequest httpUriRequest = requestCaptor.getValue();

        Assert.equals("PUT", httpUriRequest.getMethod());
        try {
            Assert.equals("http://localhost:123/api/1.x/case/note/uid/1371858/service/blaService/item/fooID",
                    httpUriRequest.getURI().toURL().toString());
        } catch (MalformedURLException e) {
            Assert.fail("exception=" + e.getMessage());
        }

        ListF<Header> sk = Cf.list(httpUriRequest.getHeaders("Secure-Key"));
        Assert.sizeIs(1, sk);
        Assert.equals(secureKey, sk.first().getValue());

        ListF<Header> ex = Cf.list(httpUriRequest.getHeaders("Expires"));
        Assert.sizeIs(1, ex);
        Assert.equals("Fri, 22 Nov 2013 20:05:29 GMT", ex.first().getValue());

        ListF<Header> ct = Cf.list(httpUriRequest.getHeaders("Content-Type"));
        Assert.sizeIs(1, ct);
        Assert.equals("application/json; charset=UTF-8", ct.first().getValue());
    }

    @Test
    public void validatePanelApiUrlHostOk() {
        HttpClient httpClientMock = mock(HttpClient.class);

        new PanelApiClient("localhost", httpClientMock);
        new PanelApiClient("localhost:234", httpClientMock);
        new PanelApiClient("127.0.0.1:234", httpClientMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatePanelApiUrlHostFail() {
        HttpClient httpClientMock = mock(HttpClient.class);

        new PanelApiClient("localhost:234:324", httpClientMock);
    }
}
