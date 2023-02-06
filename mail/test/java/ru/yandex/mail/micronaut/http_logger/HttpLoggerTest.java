package ru.yandex.mail.micronaut.http_logger;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

import java.util.List;

import static ru.yandex.mail.micronaut.http_logger.LogAssertions.assertThat;
import static ru.yandex.mail.micronaut.http_logger.LogEventAssertions.assertThat;
import static ru.yandex.mail.micronaut.http_logger.TestClient.ID;

interface TestAPI {
    @Post("/simple")
    String request(@QueryValue int password, @QueryValue List<String> strs, @Header("X-Test") String header,
                   @Header("X-Sensitive") String sensitiveHeader, @Body String body);
}

@Controller
@Produces(MediaType.TEXT_PLAIN)
class TestController implements TestAPI {
    @Override
    public String request(int password, List<String> strs, String header, String sensitiveHeader, String body) {
        return String.valueOf(password);
    }
}

@Client(id = ID)
@Consumes(MediaType.TEXT_PLAIN)
interface TestClient extends TestAPI {
    String ID = "test-client";
}

@Filter("/**")
class HeadersCopyFilter implements HttpServerFilter {
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        val requestHeaders = request.getHeaders().asMap();
        return Mono.from(chain.proceed(request))
            .map(response -> {
                return response.headers(headers -> {
                    requestHeaders.forEach((name, values) -> {
                        for (val value : values) {
                            headers.add(name, value);
                        }
                    });
                });
            });
    }
}

@MicronautTest(transactional = false)
@SuppressWarnings("ClassWithMultipleLoggers")
class HttpLoggerTest {
    private static final Logger CLIENT_LOG = LogManager.getLogger(TestClient.ID);
    private static final Logger SERVER_LOG = LogManager.getLogger("server");

    @Inject
    TestClient client;

    @Test
    @DisplayName("Verify that log record contains expected lines")
    void testLogging() {
        val response = client.request(42, List.of("1", "2"), "test", "secret", "body");

        assertThat(CLIENT_LOG).containsEventSatisfying(
            event -> assertThat(event)
                .containsLineMatching("^Sending HTTP request: POST /simple\\?.*password=<cut>.*$")
                .containsLineMatching("^Sending HTTP request: POST /simple\\?.*strs=1,2.*$")
                .containsLine("X-Test: test")
                .containsLine("X-Sensitive: <cut>")
                .containsLine("body")
                .doesNotContainThrowable()
        );

        assertThat(CLIENT_LOG).containsEventSatisfying(
            event -> assertThat(event)
                .containsLine("Response for: POST /simple")
                .containsLine("Status code: 200 (OK)")
                .containsLine("X-Test: test")
                .containsLine("X-Sensitive: <cut>")
                .containsLine(response)
                .doesNotContainThrowable()
        );

        assertThat(SERVER_LOG).containsEventSatisfying(
            event -> assertThat(event)
                .containsLineMatching("^Receiving HTTP request: POST /simple\\?.*password=<cut>.*$")
                .containsLineMatching("^Receiving HTTP request: POST /simple\\?.*strs=1,2.*$")
                .containsLine("X-Test: test")
                .containsLine("X-Sensitive: <cut>")
                .containsLine("body")
                .doesNotContainThrowable()
        );

        assertThat(SERVER_LOG).containsEventSatisfying(
            event -> assertThat(event)
                .containsLine("Response for: POST /simple")
                .containsLine("Status code: 200 (OK)")
                .containsLine("X-Test: test")
                .containsLine("X-Sensitive: <cut>")
                .containsLine(response)
                .doesNotContainThrowable()
        );
    }
}
