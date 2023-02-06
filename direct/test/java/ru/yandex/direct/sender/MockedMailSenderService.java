package ru.yandex.direct.sender;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedMailSenderService extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(MockedMailSenderService.class);
    private static final Pattern SEND_ENDPOINT_PATTERN = Pattern.compile("/api/0/(\\w+)/transactional/(\\w+)/send");

    private static final String ANSWER_OK = "{  \"result\":{\"status\":\"OK\"}  }";

    private final String token;
    private MockWebServer server;
    private List<LoggedSenderRequest> activityLog;

    public MockedMailSenderService(String authToken) {
        this.token = authToken;
    }

    private MockResponse errorResponse(int code, String message) {
        return new MockResponse()
                .setResponseCode(code)
                .setBody("{  \"result\":{\"status\":\"ERROR\", \"message\":\"" + message + "\"}  }");
    }

    @Override
    protected void before() throws Throwable {
        server = new MockWebServer();
        activityLog = new ArrayList<>();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                HttpUrl http = HttpUrl.parse("https://localhost" + request.getPath());
                if (!isValidAuth(request)) {
                    return errorResponse(403, "access denied");
                }
                String email = http.queryParameter("to_email");
                if (email == null || email.isEmpty()) {
                    return errorResponse(500, "email is mandatory");
                }
                String path = http.encodedPath();
                Matcher m = SEND_ENDPOINT_PATTERN.matcher(path);
                if (m.matches()) {
                    LoggedSenderRequest loggedRequest = new LoggedSenderRequest(m.group(1),
                            m.group(2), email);
                    activityLog.add(loggedRequest);
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody(ANSWER_OK);
                } else {
                    return errorResponse(404, "Not found");
                }
            }
        });
    }

    private boolean isValidAuth(RecordedRequest request) {
        String authHeader = request.getHeader("Authorization");
        String[] authParts = authHeader.split(" ");
        if (authParts.length == 2) {
            String authScheme = authParts[0];
            if ("Basic".equals(authScheme)) {
                byte[] decodedBytes = DatatypeConverter.parseBase64Binary(authParts[1]);
                String decoded = new String(decodedBytes, 0, decodedBytes.length - 1);
                return decoded.equals(token);
            }
        }
        return false;
    }

    public List<LoggedSenderRequest> getActivityLog() {
        return activityLog;
    }

    @Override
    protected void after() {
        try {
            server.shutdown();
        } catch (IOException ex) {
            logger.error("can't stop mocked web server", ex);
        } finally {
            server = null;
        }
    }

    public String getBaseUrl() {
        URI uri = server.url("/").uri();
        return uri.getHost() + ":" + uri.getPort();
    }

    public static class LoggedSenderRequest {
        private final String accountSlug;
        private final String campaignSlug;
        private final String toEmail;


        public LoggedSenderRequest(String accountSlug, String campaignSlug, String toEmail) {
            this.accountSlug = accountSlug;
            this.campaignSlug = campaignSlug;
            this.toEmail = toEmail;
        }

        public String getAccountSlug() {
            return accountSlug;
        }

        public String getCampaignSlug() {
            return campaignSlug;
        }

        public String getToEmail() {
            return toEmail;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LoggedSenderRequest that = (LoggedSenderRequest) o;
            return Objects.equals(accountSlug, that.accountSlug) &&
                    Objects.equals(campaignSlug, that.campaignSlug) &&
                    Objects.equals(toEmail, that.toEmail);
        }

        @Override
        public int hashCode() {

            return Objects.hash(accountSlug, campaignSlug, toEmail);
        }
    }
}
