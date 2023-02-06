package ru.yandex.direct.test.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assume.assumeNoException;

/**
 * Для junit4
 */
public class MockedHttpWebServerRule extends MockedHttpWebServerBase implements TestRule {
    public MockedHttpWebServerRule(String requestPath, String responseBody) {
        super(requestPath, responseBody);
    }

    public MockedHttpWebServerRule(String requestPath, String responseBody, ContentType contentType) {
        super(requestPath, responseBody, contentType);
    }

    public MockedHttpWebServerRule(ContentType contentType) {
        super(contentType);
    }

    public MockedHttpWebServerRule(Map<String, String> requestToResponse, ContentType contentType) {
        super(requestToResponse, contentType);
    }

    protected void before() throws Throwable {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String path = request.getPath();
                List<PredicateResponsePair> pairs = pathToPredicateAndResponse.get(path);
                if (pairs == null) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.SC_NOT_FOUND)
                            .setBody("Invalid request path");
                }

                String requestBody = request.getBody().readUtf8();
                for (PredicateResponsePair pair : pairs) {
                    if (pair.predicate.test(requestBody)) {
                        addRequestBody(path, requestBody);
                        return new MockResponse()
                                .addHeader(HttpHeaders.CONTENT_TYPE, contentType.getMimeType())
                                .setBody(pair.responseBody.apply(requestBody));
                    }
                }

                String msg = "No predicates found for request body: " + requestBody;
                logger.error(msg);
                return new MockResponse()
                        .setResponseCode(HttpStatus.SC_BAD_REQUEST)
                        .setBody(msg);
            }
        });

        try {
            server.start();
        } catch (IOException e) {
            assumeNoException(e);
        }
    }

    protected void after() {
        try {
            server.shutdown();
        } catch (IOException e) {
            logger.error("Can't shutdown server", e);
        } finally {
            server = null;
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }
}
