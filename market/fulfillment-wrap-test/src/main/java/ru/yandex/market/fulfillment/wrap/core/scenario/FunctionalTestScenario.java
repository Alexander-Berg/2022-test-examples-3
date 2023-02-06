package ru.yandex.market.fulfillment.wrap.core.scenario;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static ru.yandex.market.fulfillment.wrap.core.scenario.util.RequestFactoryWrapper.wrapInBufferedRequestFactory;

public abstract class FunctionalTestScenario<Request, Response> {

    protected final MockRestServiceServer mockServer;

    public FunctionalTestScenario(RestTemplate restTemplate) {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);

        wrapInBufferedRequestFactory(restTemplate);
    }

    public abstract void configureMocks();

    public final void verifyMocks() {
        mockServer.verify();
        mockServer.reset();
    }

    public abstract String getRequestContent();

    public abstract void doAssertions(SoftAssertions assertions,
                                      Request request,
                                      Response response);

    protected final String extractFileContent(String relativePath) {
        try {
            return IOUtils.toString(getSystemResourceAsStream(relativePath),
                    "UTF-8"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
