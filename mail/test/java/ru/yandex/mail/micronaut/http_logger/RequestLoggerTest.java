package ru.yandex.mail.micronaut.http_logger;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.mail.micronaut.http_logger.config.LoggingOptions;
import ru.yandex.mail.micronaut.http_logger.http_logger_log4j2.Log4j2RequestLoggerFactory;
import ru.yandex.mail.micronaut.http_logger.slf4j.Slf4jRequestLoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.micronaut.http_logger.LogAssertions.assertThat;
import static ru.yandex.mail.micronaut.http_logger.LogEventAssertions.assertThat;
import static ru.yandex.mail.micronaut.http_logger.LogUtils.findAppenders;

class RequestLoggerTest {
    private static final String TEST_LOG_NAME = "test";
    private static final Logger TEST_LOG = LogManager.getLogger(TEST_LOG_NAME);
    private static final List<ListAppender> TEST_APPENDERS = findAppenders(TEST_LOG, ListAppender.class);

    private static final String CUSTOM_SENSITIVE_HEADER = "X-Sensitive";
    private static final String CUSTOM_SENSITIVE_PARAM = "password";

    @BeforeEach
    void reset() {
        TEST_APPENDERS.forEach(ListAppender::clear);
    }

    private static LoggingOptions mockOptions(boolean allEnabled) {
        val options = mock(LoggingOptions.class);
        when(options.isEnabled()).thenReturn(true);
        when(options.getSensitiveHeaders()).thenReturn(singleton(CUSTOM_SENSITIVE_HEADER));
        when(options.getSensitiveParams()).thenReturn(singleton(CUSTOM_SENSITIVE_PARAM));
        when(options.isLogRequestBody()).thenReturn(allEnabled);
        when(options.isLogRequestHeaders()).thenReturn(allEnabled);
        when(options.isLogRequestParams()).thenReturn(allEnabled);
        when(options.isLogResponseBody()).thenReturn(allEnabled);
        when(options.isLogResponseHeaders()).thenReturn(allEnabled);
        return options;
    }

    static Stream<Arguments> loggerFactories() {
        return Stream.of(
            arguments("slf4j", new Slf4jRequestLoggerFactory()),
            arguments("log4j2", new Log4j2RequestLoggerFactory())
        );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger writes full request information while all options are enabled")
    void testRequestLoggingWhileAllEnabled(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val request = HttpRequest.POST("/foo/bar", "body")
            .headers(headers -> {
                headers.add("X-Test", "test");
                headers.add("X-Test", "prod");
                headers.add("Content-Length", "42");
            });
        request.getParameters().add("param", "value");

        logger.logRequest(request, "");

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLineMatching(".*POST\\s/foo/bar\\?param=value$")
                    .containsLine("X-Test: test, prod")
                    .containsLine("Content-Length: 42")
                    .containsLine("body")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger writes minimal information while all options are disabled")
    void testRequestLoggingWhileAllDisabled(String name, RequestLoggerFactory factory) {
        val options = mockOptions(false);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val request = HttpRequest.POST("/foo/bar", "body")
            .headers(headers -> {
                headers.add("X-Test", "test");
                headers.add("X-Test", "prod");
                headers.add("Content-Length", "42");
            });
        request.getParameters().add("param", "value");

        logger.logRequest(request, "");

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .doesNotContainLineMatching(".*POST\\s/foo/bar\\?param=value$")
                    .containsLineMatching(".*POST\\s/foo/bar$")
                    .doesNotContainLine("X-Test: test, prod")
                    .doesNotContainLine("Content-Length: 42")
                    .doesNotContainLine("body")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger writes full response information while all options are enabled")
    void testResponseLoggingWhileAllEnabled(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val response = HttpResponse.ok("body")
            .headers(headers -> {
                headers.add("X-Test", "test");
            });

        logger.logResponse(HttpMethod.GET, "/foo/bar", response);

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLineMatching(".*GET\\s/foo/bar$")
                    .containsLine("Status code: 200 (OK)")
                    .containsLine("X-Test: test")
                    .containsLine("body")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger writes minimal response information while all options are disabled")
    void testResponseLoggingWhileAllDisabled(String name, RequestLoggerFactory factory) {
        val options = mockOptions(false);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val response = HttpResponse.ok("body")
            .headers(headers -> {
                headers.add("X-Test", "test");
            });

        logger.logResponse(HttpMethod.GET, "/foo/bar", response);

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLineMatching(".*GET\\s/foo/bar$")
                    .containsLine("Status code: 200 (OK)")
                    .doesNotContainLine("X-Test: test")
                    .doesNotContainLine("body")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger writes error information if response failed")
    void testResponseErrorLogging(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val errorMsg = "Achtung!! Something is going wrong!";
        logger.logResponse(HttpMethod.GET, "/foo/bar", new RuntimeException(errorMsg));

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLineMatching(".*GET\\s/foo/bar$")
                    .doesNotContainLineMatching("^Status code:\\s\\d\\s.*$")
                    .containsThrowableSatisfying(e -> {
                        assertThat(e)
                            .isExactlyInstanceOf(RuntimeException.class)
                            .hasMessageContainingAll(errorMsg);
                    })
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger replaces request sensitive headers")
    void testRequestLoggingSensitiveHeaders(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val request = HttpRequest.GET("/foo/bar")
            .headers(headers -> {
                headers.add("X-Test", "test");
                headers.add(CUSTOM_SENSITIVE_HEADER, "abyrvalg");
                headers.add("Authorization", "my super strong password");
            });

        logger.logRequest(request, "");

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLine("Authorization: <cut>")
                    .containsLine(CUSTOM_SENSITIVE_HEADER + ": <cut>")
                    .containsLine("X-Test: test")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger replaces request sensitive parameters")
    void testRequestLoggingSensitiveParameters(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val request = HttpRequest.GET("/foo/bar");
        request.getParameters()
            .add("baz", "42")
            .add(CUSTOM_SENSITIVE_PARAM, "super secret password");

        logger.logRequest(request, "");

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLineMatching("^.*GET\\s/foo/bar\\?.*baz=42.*$")
                    .containsLineMatching("^.*GET\\s/foo/bar\\?.*" + CUSTOM_SENSITIVE_PARAM + "=<cut>.*$")
                    .doesNotContainThrowable()
            );
    }

    @ParameterizedTest
    @MethodSource("loggerFactories")
    @DisplayName("Verify that '{0}' request-logger replaces response sensitive headers")
    void testResponseLoggingSensitiveHeaders(String name, RequestLoggerFactory factory) {
        val options = mockOptions(true);
        val logger = factory.createLogger(TEST_LOG_NAME, options);

        val response = HttpResponse.ok()
            .headers(headers -> {
                headers.add("X-Test", "test");
                headers.add(CUSTOM_SENSITIVE_HEADER, "abyrvalg");
                headers.add("Authorization", "my super strong password");
            });

        logger.logResponse(HttpMethod.GET, "/foo/bar", response);

        assertThat(TEST_LOG)
            .containsEventSatisfying(
                event -> assertThat(event)
                    .containsLine("Authorization: <cut>")
                    .containsLine(CUSTOM_SENSITIVE_HEADER + ": <cut>")
                    .containsLine("X-Test: test")
                    .doesNotContainThrowable()
            );
    }
}
