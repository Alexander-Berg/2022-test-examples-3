package ru.yandex.direct.turbolandings.client;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class TurboLandingsClientTestBase {
    private final static Logger logger = LoggerFactory.getLogger(TurboLandingsClientTestBase.class);
    static final String TEST_TURBO_LANDINGS_TOKEN = "s3cr3t";
    static final String TEST_TURBO_LANDINGS_FILE = "";

    private MockWebServer mockWebServer;
    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    TurboLandingsClient turboLandingsClient;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        turboLandingsClient = new TurboLandingsClient(
                new TurboLandingsClientConfiguration(url(), TEST_TURBO_LANDINGS_TOKEN, TEST_TURBO_LANDINGS_FILE),
                parallelFetcherFactory,
                Duration.ofSeconds(20));
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
