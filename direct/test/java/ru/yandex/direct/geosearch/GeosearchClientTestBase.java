package ru.yandex.direct.geosearch;

import java.io.IOException;
import java.time.Duration;

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
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class GeosearchClientTestBase {
    static final String TICKET_BODY = "ticketBody";
    private final static Logger logger = LoggerFactory.getLogger(GeosearchClientTestBase.class);

    private MockWebServer mockWebServer;
    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;
    public DirectConfig directConfig;

    TvmIntegration tvmIntegration;

    GeosearchClient geosearchClient;

    @Before
    public void setUp() throws IOException {

        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        directConfig = DirectConfigFactory.getConfig();

        tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(TvmService.DUMMY)).thenReturn(TICKET_BODY);

        DirectConfig config = directConfig.getBranch("geosearch");
        String origin = config.getString("origin");
        Duration requestTimeout = config.getDuration("request_timeout");
        int parallel = config.getInt("parallel");
        int retryCount = config.getInt("request_retries");
        int tvmAppId = config.getInt("tvm_app_id");
        GeosearchClientSettings settings = new GeosearchClientSettings(url(), origin, retryCount, requestTimeout,
                parallel, tvmAppId);

        geosearchClient = new GeosearchClient(settings, TvmService.DUMMY, parallelFetcherFactory, tvmIntegration);
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
