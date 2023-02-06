package ru.yandex.direct.turboapps.client;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class TurboAppsClientTestBase {
    static final String TICKET_BODY = "ticketBody";
    private final static Logger logger = LoggerFactory.getLogger(TurboAppsClientTestBase.class);

    private MockWebServer mockWebServer;
    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    TurboAppsClient turboappsClient;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(TvmService.DUMMY)).thenReturn(TICKET_BODY);

        turboappsClient = new TurboAppsClient(url(), 100, tvmIntegration, TvmService.DUMMY, parallelFetcherFactory);
    }

    protected abstract Dispatcher dispatcher();

    protected String url() {
        return "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
