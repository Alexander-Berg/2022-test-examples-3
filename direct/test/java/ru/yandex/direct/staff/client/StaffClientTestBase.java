package ru.yandex.direct.staff.client;

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
import ru.yandex.direct.staff.client.model.StaffConfiguration;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class StaffClientTestBase {
    private static final String TICKET_BODY = "ticketBody";
    private final static Logger logger = LoggerFactory.getLogger(StaffClientTestBase.class);

    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();
    StaffClient staffClient;
    @Autowired
    private ParallelFetcherFactory parallelFetcherFactory;
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(any(TvmService.class))).thenReturn(TICKET_BODY);

        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();
        String mockWebServerUrl = "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
        StaffConfiguration staffConfiguration = new StaffConfiguration(mockWebServerUrl, mockWebServerUrl, null);
        staffClient = new StaffClient(staffConfiguration, parallelFetcherFactory, tvmIntegration, false);
    }

    protected abstract Dispatcher dispatcher();

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
