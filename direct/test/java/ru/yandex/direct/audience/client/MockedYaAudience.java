package ru.yandex.direct.audience.client;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.liveresource.MemoryLiveResource;

public class MockedYaAudience extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(MockedYaAudience.class);

    private MockWebServer server;

    private static final String CREATE_EXPERIMENT_SUCCESS_REQUEST =
            "POST:/v1/management/experiments?ulogin=direct-client:{\"experiment\":{\"name\":\"Brand-lift 123337516\"," +
                    "\"counter_ids\":[56089156],\"segments\":[{\"name\":\"A\",\"start\":0,\"end\":90}," +
                    "{\"name\":\"B\",\"start\":90,\"end\":100}]}}";
    private static final String CREATE_EXPERIMENT_SUCCESS_RESPONSE =
            "{\"experiment\":{\"experiment_id\":945,\"segments\":[{\"segment_id\":3031},{\"segment_id\":3032}]}}";

    private static final String SET_EXPERIMENT_GRANT_SUCCESS_REQUEST =
            "PUT:/v1/management/experiment/945/grant:{\"grant\":{\"user_login\":\"brand-lift-login\"," +
                    "\"permission\":\"view\"}}";
    private static final String SET_EXPERIMENT_GRANT_SUCCESS_RESPONSE =
            "{\"grant\":{\"permission\":\"view\"}}";

    private static final String CONFIRM_SEGMENT_SUCCESS_REQUEST =
            "POST:/v1/management/client/segment/3031/confirm?ulogin=direct-client:{\"segment\":{\"id\":3031," +
                    "\"name\":\"A\",\"content_type\":\"yuid\",\"hashed\":false}}";
    private static final String CONFIRM_SEGMENT_SUCCESS_RESPONSE =
            "{\"segment\":{\"id\":3031,\"status\":\"uploaded\",\"name\":\"A\"}}";


    @Override
    protected void before() throws Throwable {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String req = request.getMethod() + ":" + request.getPath();
                String body = request.getBody().readUtf8();
                if (body.length() > 0) {
                    req = req + ":" + body;
                }
                switch (req) {
                    case CREATE_EXPERIMENT_SUCCESS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(CREATE_EXPERIMENT_SUCCESS_RESPONSE);
                    case SET_EXPERIMENT_GRANT_SUCCESS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(SET_EXPERIMENT_GRANT_SUCCESS_RESPONSE);
                    case CONFIRM_SEGMENT_SUCCESS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(CONFIRM_SEGMENT_SUCCESS_RESPONSE);
                }
                logger.error("UNEXPECTED REQUEST: {}", req);
                return new MockResponse().setResponseCode(404).setBody("Request not supported");
            }
        });
        server.start();
    }

    @Override
    protected void after() {
        try {
            server.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBaseUrl() {
        return server.url("/").toString();
    }

    public YaAudienceClient createClient() {
        return new YaAudienceClient(new DefaultAsyncHttpClient(), new MemoryLiveResource(""), getBaseUrl());
    }
}
