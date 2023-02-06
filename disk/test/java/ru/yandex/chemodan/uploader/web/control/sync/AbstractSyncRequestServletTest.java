package ru.yandex.chemodan.uploader.web.control.sync;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.chemodan.uploader.processor.SyncRequestProcessor;
import ru.yandex.chemodan.uploader.registry.MulcaDownloadException;
import ru.yandex.chemodan.util.exception.PermanentHttpFailureException;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.ip.IpPortUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class AbstractSyncRequestServletTest {

    private static final int PORT = IpPortUtils.getFreeLocalPort().getPort();

    private static final String SERVER_URL = "http://localhost:" + PORT;
    private static final MulcaId FAKE_MULCA_ID = MulcaId.fromSerializedString("123456");

    private Server server;

    @Mock
    private SyncRequestProcessor processorMock;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        AbstractSyncRequestServlet servlet = new AbstractSyncRequestServlet() {
            protected void handleRequest(HttpServletRequestX reqX, HttpServletResponse resp) throws IOException {
                processorMock.recalcDigests(FAKE_MULCA_ID, true, true, true);
            }
        };
        servlet.setTimeout(Duration.standardSeconds(10L));
        servlet.setProcessor(processorMock);

        server = new Server(PORT);
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(servlet), "/*");
        server.setHandler(handler);
        server.start();
    }

    @After
    public void destroy() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void shouldHandleRequests() {
        expectHttpCode(HttpStatus.SC_200_OK);
        verify(processorMock).recalcDigests(FAKE_MULCA_ID, true, true, true);
    }

    @Test
    public void shouldReturn404IfMulcaIdNotFound() {
        when(processorMock.recalcDigests(FAKE_MULCA_ID, true, true, true))
                .thenThrow(new PermanentHttpFailureException("", 404));
        expectHttpCode(HttpStatus.SC_404_NOT_FOUND);
    }

    @Test
    public void shouldReturn503IfMulcaNotAvailable() {
        when(processorMock.recalcDigests(FAKE_MULCA_ID, true, true, true))
                .thenThrow(new MulcaDownloadException(new Exception(), FAKE_MULCA_ID));
        expectHttpCode(HttpStatus.SC_503_SERVICE_UNAVAILABLE);
    }

    private void expectHttpCode(final int code) {
        ApacheHttpClientUtils.execute(new HttpGet(SERVER_URL), response -> {
            Assert.equals(code, response.getStatusLine().getStatusCode());
            return null;
        });
    }
}
