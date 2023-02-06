package ru.yandex.direct.captcha.passport;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedCaptchaService extends ExternalResource {

    private static final Logger logger = LoggerFactory.getLogger(MockedCaptchaService.class);

    private static final String CAPTCHA_GENERATE_RESPONSE = "<?xml version=\"1.0\"?>\n"
            + "<number url='http://s.captcha.yandex.net/image?key=%s'>%s</number>";


    private static final String CAPTCHA_CHECK_RESPONSE = "<?xml version='1.0'?>\n"
            + "<image_check>ok</image_check>";

    private static final String CAPTCHA_CHECK_WRONG_REP_RESPONSE = "<?xml version='1.0'?>\n"
            + "<image_check>failed</image_check>";

    private static final String CAPTCHA_CHECK_NOT_FOUND_RESPONSE = "<?xml version='1.0'?>\n"
            + "<image_check error=\"not found\">failed</image_check>";
    public static final String BODY_CONTENT_TYPE = "text/xml; charset=utf-8";


    private MockWebServer server;
    private final ImmutableMap<String, String> solvedCaptchas;

    public MockedCaptchaService(Map<String, String> solvedCaptchas) {
        Preconditions.checkArgument(!(solvedCaptchas == null && solvedCaptchas.isEmpty()));
        this.solvedCaptchas = ImmutableMap.copyOf(solvedCaptchas);
    }

    @Override
    protected void before() throws Throwable {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                HttpUrl http = HttpUrl.parse("http://localhost" + request.getPath());
                switch (http.encodedPath()) {
                    case "/generate":
                        String generatedCaptchaKey = solvedCaptchas.keySet().iterator().next();
                        String body =
                                String.format(CAPTCHA_GENERATE_RESPONSE, generatedCaptchaKey, generatedCaptchaKey);
                        return new MockResponse().addHeader(HttpHeaders.CONTENT_TYPE, BODY_CONTENT_TYPE)
                                .setBody(body);
                    case "/check":
                        String captchaKey = http.queryParameter("key");
                        String captchaRep = http.queryParameter("rep");

                        if (solvedCaptchas.containsKey(captchaKey)) {
                            String correctCaptchaSolution = solvedCaptchas.get(captchaKey);
                            if (correctCaptchaSolution.equals(captchaRep)) {
                                return new MockResponse()
                                        .addHeader(HttpHeaders.CONTENT_TYPE, BODY_CONTENT_TYPE)
                                        .setBody(CAPTCHA_CHECK_RESPONSE);
                            } else {
                                return new MockResponse()
                                        .addHeader(HttpHeaders.CONTENT_TYPE, BODY_CONTENT_TYPE)
                                        .setBody(CAPTCHA_CHECK_WRONG_REP_RESPONSE);
                            }
                        } else {
                            return new MockResponse()
                                    .addHeader(HttpHeaders.CONTENT_TYPE, BODY_CONTENT_TYPE)
                                    .setBody(CAPTCHA_CHECK_NOT_FOUND_RESPONSE);
                        }
                }
                logger.error("UNEXPECTED REQUEST: {}", request);
                return new MockResponse()
                        .setResponseCode(404)
                        .setBody("Request not supported");
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

    public String getBaseUrl() {
        URI uri = server.url("/").uri();
        return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
    }
}
