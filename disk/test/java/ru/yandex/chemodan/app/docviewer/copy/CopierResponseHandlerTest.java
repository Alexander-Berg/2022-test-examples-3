package ru.yandex.chemodan.app.docviewer.copy;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.convert.MimeDetector;
import ru.yandex.chemodan.app.docviewer.states.MaxFileSizeCheckerImpl;
import ru.yandex.chemodan.app.docviewer.utils.HttpUtils2;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

public class CopierResponseHandlerTest extends TestBase {

    @Mock
    private MimeDetector mimeDetector = new MimeDetector();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mimeDetector.getMimeType(Mockito.any())).thenReturn(MimeTypes.MIME_UNKNOWN);
    }

    @Test
    public void checkLastAccessHeaderHandling() throws Exception {
        long lastAccessSecs = 1410694291L;
        CopierResponseHandler handler = createResponseHandler();
        HttpResponse resp = createFakeResponse(lastAccessSecs);

        TempFileInfo info = handler.handleResponse(resp);
        Assert.equals(lastAccessSecs * 1000, info.getSerpLastAccess().get().getMillis());
    }

    private HttpResponse createFakeResponse(long lastAccessSecs) throws UnsupportedEncodingException {
        HttpResponse resp = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
        resp.setEntity(new StringEntity("text"));
        resp.setHeader(HttpUtils2.HEADER_LAST_ACCESS, Long.valueOf(lastAccessSecs).toString());
        return resp;
    }

    private CopierResponseHandler createResponseHandler() throws URISyntaxException {
        MaxFileSizeCheckerImpl checker = new MaxFileSizeCheckerImpl();
        checker.setMaxLengthsForTests(Tuple2.tuple(DataSize.fromBytes(100), DataSize.fromBytes(100)));
        return new CopierResponseHandler(new URI("fake.ru"), Option.empty(), checker, Option.empty(), mimeDetector);
    }
}
