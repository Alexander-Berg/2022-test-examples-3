package ru.yandex.direct.http.smart.examples;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.http.smart.core.Smart;

@ContextConfiguration(classes = {TestingConfiguration.class})
@ExtendWith({SoftAssertionsExtension.class, SpringExtension.class})
public abstract class MockServerBase {
    private final static Logger logger = LoggerFactory.getLogger(MockServerBase.class);
    private MockWebServer mockWebServer;

    @InjectSoftAssertions
    protected SoftAssertions softAssertions;

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    protected Smart.Builder builder() {
        return Smart.builder()
                .withParallelFetcherFactory(parallelFetcherFactory)
                .withProfileName("test")
                .withBaseUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort());
    }

    ;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();
    }

    public abstract Dispatcher dispatcher();

    @AfterEach
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
