package ru.yandex.direct.turbopages.client;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class TurbopagesClientTestBase {
    private final static Logger logger = LoggerFactory.getLogger(TurbopagesClientTestBase.class);

    private MockWebServer mockWebServer;
    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    TurbopagesClient turbopagesClient;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        turbopagesClient = new TurbopagesClient(new TurbopagesClientConfiguration(url(), "header"),
                parallelFetcherFactory);
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
