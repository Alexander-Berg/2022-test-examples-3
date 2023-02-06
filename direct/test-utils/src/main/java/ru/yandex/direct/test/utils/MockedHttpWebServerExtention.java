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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Для junit5
 */
public class MockedHttpWebServerExtention extends MockedHttpWebServerBase implements BeforeEachCallback,
        AfterEachCallback {
    public MockedHttpWebServerExtention(String requestPath, String responseBody) {
        super(requestPath, responseBody);
    }

    public MockedHttpWebServerExtention(String requestPath, String responseBody, ContentType contentType) {
        super(requestPath, responseBody, contentType);
    }

    public MockedHttpWebServerExtention(ContentType contentType) {
        super(contentType);
    }

    public MockedHttpWebServerExtention(Map<String, String> requestToResponse, ContentType contentType) {
        super(requestToResponse, contentType);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
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
            throw new RuntimeException("Mocked server throws exception");
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            server.shutdown();
            pathToRequestBody.clear();
            pathToPredicateAndResponse.clear();
        } catch (IOException e) {
            logger.error("Can't shutdown server", e);
        } finally {
            server = null;
        }
    }

}
